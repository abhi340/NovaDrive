package com.example.tvdrive.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

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

    // Scopes supported by Google OAuth 2.0 Device Flow (RFC 8628)
    private val scopes = listOf(
        "https://www.googleapis.com/auth/drive.readonly",
        "https://www.googleapis.com/auth/photoslibrary.readonly",
        "email",
        "profile",
        "openid"
    ).joinToString(" ")

    /**
     * Starts the device authorization flow:
     * 1. Obtains user_code & device_code from Google RFC 8628 endpoint.
     * 2. Emits CodeReady with the real Google user_code for QR and on-screen display.
     * 3. Polls Google token endpoint until user approves on phone/computer.
     */
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

        // Poll for user authorization
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

                    // Fetch real user profile and email from Google userinfo API
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

                    // Also link with Firebase Auth if id_token present
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
                        // User hasn't finished yet — continue polling
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
                // Network glitch — retry on next interval
            }
        }

        _deviceFlowState.value = DeviceFlowState.Error("Session timed out. Please try again.")
    }


    fun cancel() {
        pollingJob?.cancel()
        _deviceFlowState.value = DeviceFlowState.Idle
    }
}
