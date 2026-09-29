package com.example.tvdrive.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
private const val PHOTOS_SCOPE = "https://www.googleapis.com/auth/photoslibrary.readonly"
/** Combined OAuth2 scope string for GoogleAuthUtil.getToken() */
const val COMBINED_OAUTH_SCOPE = "oauth2:$DRIVE_SCOPE $PHOTOS_SCOPE"

val FIREBASE_WEB_CLIENT_ID: String get() = com.example.tvdrive.BuildConfig.FIREBASE_WEB_CLIENT_ID
val GOOGLE_TV_CLIENT_ID: String get() = com.example.tvdrive.BuildConfig.GOOGLE_TV_CLIENT_ID
val GOOGLE_TV_CLIENT_SECRET: String get() = com.example.tvdrive.BuildConfig.GOOGLE_TV_CLIENT_SECRET

private const val PREFS_NAME = "tvdrive_auth"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_EXPIRES_AT = "expires_at"
private const val KEY_EMAIL = "email"

class AuthManager(private val context: Context) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val refreshHttpClient = OkHttpClient()

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(FIREBASE_WEB_CLIENT_ID)
            .requestScopes(Scope(DRIVE_SCOPE), Scope(PHOTOS_SCOPE))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    private var manualToken: String? = null

    init {
        val storedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val storedRefresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        val storedEmail = prefs.getString(KEY_EMAIL, "Google User") ?: "Google User"

        if (!storedToken.isNullOrBlank() || !storedRefresh.isNullOrBlank()) {
            manualToken = storedToken
            _authState.value = AuthState.SignedIn(
                account = null,
                email = storedEmail,
                directAccessToken = storedToken
            )
        } else {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            _authState.value = if (account != null && hasRequiredScopes(account)) {
                AuthState.SignedIn(account)
            } else {
                AuthState.SignedOut
            }
        }
    }

    fun getSignInIntent(): Intent = signInClient.signInIntent

    suspend fun handleSignInResult(data: Intent?): AuthState {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                try {
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential).await()
                } catch (_: Exception) {}
            }
            AuthState.SignedIn(account).also { _authState.value = it }
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Sign-in failed").also { _authState.value = it }
        }
    }

    fun setDeviceAuthorizedToken(
        token: String,
        refreshToken: String?,
        expiresInSeconds: Int,
        userEmail: String
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            if (!refreshToken.isNullOrBlank()) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            putLong(KEY_EXPIRES_AT, expiresAt)
            putString(KEY_EMAIL, userEmail)
            apply()
        }
        manualToken = token
        _authState.value = AuthState.SignedIn(account = null, email = userEmail, directAccessToken = token)
    }

    suspend fun signOut() {
        manualToken = null
        prefs.edit().clear().apply()
        try {
            signInClient.signOut().await()
        } catch (_: Exception) {}
        _authState.value = AuthState.SignedOut
    }

    fun getCurrentAccount(): GoogleSignInAccount? =
        (_authState.value as? AuthState.SignedIn)?.account

    fun getCurrentEmail(): String =
        (_authState.value as? AuthState.SignedIn)?.email
            ?: (_authState.value as? AuthState.SignedIn)?.account?.email
            ?: "Google Account"

    /**
     * Returns a valid OAuth2 access token for the current account.
     * Checks manual token from QR Device Flow first, then GoogleAuthUtil.
     * MUST be called on Dispatchers.IO.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        getAccessTokenSync()
    }

    private var lastGoogleAuthToken: String? = null

    /**
     * Forces immediate refresh of the access token using the stored refresh token.
     */
    fun invalidateAndRefresh(): String? {
        val storedRefresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (!storedRefresh.isNullOrBlank()) {
            val refreshed = refreshAccessTokenSync(storedRefresh)
            if (refreshed != null) return refreshed
        }
        val account = getCurrentAccount()
        if (account != null && account.account != null) {
            try {
                lastGoogleAuthToken?.let { GoogleAuthUtil.clearToken(context, it) }
                val fresh = GoogleAuthUtil.getToken(context, account.account!!, COMBINED_OAUTH_SCOPE)
                lastGoogleAuthToken = fresh
                return fresh
            } catch (e: Exception) {
                android.util.Log.e("AuthManager", "Error clearing and refreshing token", e)
            }
        }
        return null
    }

    /**
     * Synchronous access token retrieval for OkHttp interceptors.
     * Automatically auto-refreshes if within 2 minutes of expiry.
     */
    fun getAccessTokenSync(): String? {
        val storedRefresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        var currentToken = manualToken ?: prefs.getString(KEY_ACCESS_TOKEN, null)

        // Check if token is near expiration or missing, and refresh token is available
        val isExpiring = (System.currentTimeMillis() >= expiresAt - 120_000L) || currentToken == null
        if (isExpiring && !storedRefresh.isNullOrBlank()) {
            val refreshed = refreshAccessTokenSync(storedRefresh)
            if (refreshed != null) {
                currentToken = refreshed
            }
        }

        if (!currentToken.isNullOrBlank()) {
            manualToken = currentToken
            return currentToken
        }

        val account = getCurrentAccount() ?: return null
        return try {
            val tok = GoogleAuthUtil.getToken(context, account.account!!, COMBINED_OAUTH_SCOPE)
            lastGoogleAuthToken = tok
            tok
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "getAccessTokenSync failed", e)
            null
        }
    }

    private fun refreshAccessTokenSync(refreshToken: String): String? {
        return try {
            android.util.Log.d("AuthManager", "Auto-refreshing OAuth access token via refresh_token...")
            val formBody = FormBody.Builder()
                .add("client_id", GOOGLE_TV_CLIENT_ID)
                .add("client_secret", GOOGLE_TV_CLIENT_SECRET)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val req = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            val resp = refreshHttpClient.newCall(req).execute()
            val bodyString = resp.body?.string()

            if (resp.isSuccessful && !bodyString.isNullOrBlank()) {
                val json = JSONObject(bodyString)
                val newAccessToken = json.getString("access_token")
                val expiresIn = json.optInt("expires_in", 3600)
                val newRefresh = json.optString("refresh_token").ifEmpty { refreshToken }
                val newExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

                prefs.edit().apply {
                    putString(KEY_ACCESS_TOKEN, newAccessToken)
                    putString(KEY_REFRESH_TOKEN, newRefresh)
                    putLong(KEY_EXPIRES_AT, newExpiresAt)
                    apply()
                }
                manualToken = newAccessToken
                android.util.Log.d("AuthManager", "OAuth access token successfully refreshed! Valid for ${expiresIn}s")
                newAccessToken
            } else {
                android.util.Log.e("AuthManager", "Token refresh failed HTTP ${resp.code}: $bodyString")
                if (resp.code == 400 || resp.code == 401) {
                    // Refresh token revoked or expired — clear and set SignedOut
                    prefs.edit().clear().apply()
                    manualToken = null
                    _authState.value = AuthState.SignedOut
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Network exception during token refresh", e)
            null
        }
    }

    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean =
        GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE), Scope(PHOTOS_SCOPE))
}

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(
        val account: GoogleSignInAccount? = null,
        val email: String = account?.email ?: "Google User",
        val directAccessToken: String? = null
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * State of the QR Code Device Sign-In Flow
 */
sealed class DeviceFlowState {
    object Idle : DeviceFlowState()
    object Loading : DeviceFlowState()
    data class CodeReady(
        val userCode: String,
        val verificationUrl: String,
        val directQrUrl: String,
        val expiresInSeconds: Int
    ) : DeviceFlowState()
    data class Success(val email: String) : DeviceFlowState()
    data class Error(val message: String) : DeviceFlowState()
}

/**
 * Manages Google OAuth 2.0 Device Authorization Grant (RFC 8628).
 * Enables users to scan a QR code with their phone camera to sign into TV Drive.
 */
class DeviceAuthManager(
    private val okHttpClient: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val _deviceFlowState = MutableStateFlow<DeviceFlowState>(DeviceFlowState.Idle)
    val deviceFlowState: StateFlow<DeviceFlowState> = _deviceFlowState.asStateFlow()

    private var pollingJob: Job? = null

    private val scopes = listOf(
        "https://www.googleapis.com/auth/drive.readonly",
        "https://www.googleapis.com/auth/photoslibrary.readonly",
        "email",
        "profile",
        "openid"
    ).joinToString(" ")

    fun startDeviceFlow(
        clientId: String = GOOGLE_TV_CLIENT_ID,
        clientSecret: String = GOOGLE_TV_CLIENT_SECRET,
        onTokenReceived: (accessToken: String, refreshToken: String?, expiresInSeconds: Int, email: String) -> Unit
    ) {
        pollingJob?.cancel()
        _deviceFlowState.value = DeviceFlowState.Loading

        pollingJob = scope.launch {
            try {
                requestGoogleDeviceCode(clientId, clientSecret, onTokenReceived)
            } catch (e: Exception) {
                _deviceFlowState.value = DeviceFlowState.Error("Google connection error: ${e.localizedMessage ?: "Check network"}")
            }
        }
    }

    private suspend fun requestGoogleDeviceCode(
        clientId: String,
        clientSecret: String,
        onTokenReceived: (String, String?, Int, String) -> Unit
    ) {
        val formBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", scopes)
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/device/code")
            .post(formBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: throw IOException("Empty response from Google Device Code endpoint")

        if (!response.isSuccessful) {
            throw IOException("Google Device Code request failed: $bodyStr")
        }

        val json = JSONObject(bodyStr)
        val deviceCode = json.getString("device_code")
        val userCode = json.getString("user_code")
        val verificationUrl = json.optString("verification_url", "https://www.google.com/device")
        val directQrUrl = json.optString("verification_url_complete", "$verificationUrl?user_code=$userCode")
        val expiresIn = json.optInt("expires_in", 1800)
        val interval = json.optInt("interval", 5)

        _deviceFlowState.value = DeviceFlowState.CodeReady(
            userCode = userCode,
            verificationUrl = verificationUrl,
            directQrUrl = directQrUrl,
            expiresInSeconds = expiresIn
        )

        pollTokenEndpoint(clientId, clientSecret, deviceCode, interval, expiresIn, onTokenReceived)
    }

    private suspend fun pollTokenEndpoint(
        clientId: String,
        clientSecret: String,
        deviceCode: String,
        intervalSeconds: Int,
        expiresInSeconds: Int,
        onTokenReceived: (String, String?, Int, String) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        val pollDelay = (intervalSeconds.coerceAtLeast(4)) * 1000L

        while (System.currentTimeMillis() - startTime < expiresInSeconds * 1000L) {
            delay(pollDelay)

            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .apply {
                    if (clientSecret.isNotBlank()) {
                        add("client_secret", clientSecret)
                    }
                }
                .add("device_code", deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                val bodyStr = response.body?.string() ?: continue

                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val accessToken = json.getString("access_token")
                    val refreshToken = json.optString("refresh_token").ifEmpty { null }
                    val tokenExpiresIn = json.optInt("expires_in", 3600)
                    val idToken = json.optString("id_token")

                    var email = "Google User"
                    try {
                        val userReq = Request.Builder()
                            .url("https://www.googleapis.com/oauth2/v3/userinfo")
                            .addHeader("Authorization", "Bearer $accessToken")
                            .build()
                        val userResp = okHttpClient.newCall(userReq).execute()
                        if (userResp.isSuccessful) {
                            val userJson = JSONObject(userResp.body?.string() ?: "{}")
                            val fetchedEmail = userJson.optString("email")
                            if (fetchedEmail.isNotBlank()) {
                                email = fetchedEmail
                            }
                        }
                    } catch (_: Exception) {}

                    if (idToken.isNotBlank()) {
                        try {
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                            if (email == "Google User") {
                                email = authResult.user?.email ?: email
                            }
                        } catch (_: Exception) {}
                    }

                    _deviceFlowState.value = DeviceFlowState.Success(email)
                    onTokenReceived(accessToken, refreshToken, tokenExpiresIn, email)
                    return
                } else {
                    val json = JSONObject(bodyStr)
                    val error = json.optString("error")
                    if (error == "authorization_pending") {
                        continue
                    } else if (error == "slow_down") {
                        delay(5000L)
                        continue
                    } else {
                        _deviceFlowState.value = DeviceFlowState.Error("Sign-in expired or denied: $error")
                        return
                    }
                }
            } catch (_: IOException) {
                // Retry on next interval
            }
        }

        _deviceFlowState.value = DeviceFlowState.Error("Session timed out. Please try again.")
    }

    fun cancel() {
        pollingJob?.cancel()
        _deviceFlowState.value = DeviceFlowState.Idle
    }
}

