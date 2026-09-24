package com.example.tvdrive.data.remote

import android.content.Context
import com.example.tvdrive.auth.AuthManager
import com.example.tvdrive.auth.COMBINED_OAUTH_SCOPE
import com.example.tvdrive.data.model.DriveFile
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

private const val DRIVE_API = "https://www.googleapis.com/drive/v3"
private const val FILE_FIELDS = "nextPageToken,files(id,name,mimeType,size,modifiedTime,thumbnailLink,webContentLink,parents)"
private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.readonly"

/**
 * Lean Drive REST client using OkHttp.
 * Replaces the 5 MB+ google-api-client JAR with direct HTTP calls.
 */
class DriveRestClient(
    private val authManager: AuthManager,
    private val okHttpClient: OkHttpClient
) {
    private suspend fun token(): String? = authManager.getAccessToken()

    private suspend fun get(url: String): JSONObject = withContext(Dispatchers.IO) {
        var tok = token() ?: run {
            android.util.Log.e("DriveRestClient", "No OAuth2 access token available")
            throw java.io.IOException("Not signed in. Please sign in with your Google account.")
        }
        var req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $tok")
            .build()
        try {
            var resp = okHttpClient.newCall(req).execute()
            if (resp.code == 401) {
                android.util.Log.w("DriveRestClient", "HTTP 401 for $url. Invalidate and refresh token...")
                val freshTok = authManager.invalidateAndRefresh()
                if (!freshTok.isNullOrBlank()) {
                    tok = freshTok
                    req = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $tok")
                        .build()
                    resp = okHttpClient.newCall(req).execute()
                }
            }
            if (!resp.isSuccessful) {
                val errBody = resp.body?.string()
                android.util.Log.e("DriveRestClient", "HTTP ${resp.code} for $url: $errBody")
                if (resp.code == 401) {
                    throw java.io.IOException("Session expired. Please sign in again.")
                }
                throw java.io.IOException("Google Drive error: HTTP ${resp.code}")
            }
            JSONObject(resp.body?.string() ?: throw java.io.IOException("Empty response from Google Drive"))
        } catch (e: Exception) {
            android.util.Log.e("DriveRestClient", "Network error for $url", e)
            throw e
        }
    }

    suspend fun listFiles(folderId: String, pageToken: String? = null): DrivePageResult {
        val q = URLEncoder.encode("'$folderId' in parents and trashed = false", "UTF-8")
        val url = buildString {
            append("$DRIVE_API/files?q=$q&fields=$FILE_FIELDS&orderBy=folder,name&pageSize=50")
            if (pageToken != null) append("&pageToken=$pageToken")
        }
        return parseFileListResult(get(url))
    }

    suspend fun searchFiles(query: String, pageToken: String? = null): DrivePageResult {
        val q = URLEncoder.encode("name contains '$query' and trashed = false", "UTF-8")
        val url = buildString {
            append("$DRIVE_API/files?q=$q&fields=$FILE_FIELDS&pageSize=30")
            if (pageToken != null) append("&pageToken=$pageToken")
        }
        return parseFileListResult(get(url))
    }

    /** Build a streaming URL for ExoPlayer / downloads (auth header required separately) */
    fun streamUrl(fileId: String): String {
        return "$DRIVE_API/files/$fileId?alt=media"
    }

    /** Provides a token for OkHttp interceptors (e.g. ExoPlayer data source) */
    suspend fun getToken(): String? = token()

    private fun parseFileListResult(json: JSONObject?): DrivePageResult {
        json ?: return DrivePageResult(emptyList(), null)
        val arr = json.optJSONArray("files") ?: return DrivePageResult(emptyList(), null)
        val files = (0 until arr.length()).map { i ->
            val f = arr.getJSONObject(i)
            DriveFile(
                id            = f.optString("id"),
                name          = f.optString("name"),
                mimeType      = f.optString("mimeType"),
                size          = f.optLong("size", 0L),
                modifiedTime  = f.optString("modifiedTime"),
                thumbnailLink = f.optString("thumbnailLink").ifEmpty { null },
                webContentLink = f.optString("webContentLink").ifEmpty { null },
                parents       = f.optJSONArray("parents")
                    ?.let { p -> (0 until p.length()).map { p.getString(it) } }
                    ?: emptyList()
            )
        }
        return DrivePageResult(files, json.optString("nextPageToken").ifEmpty { null })
    }
}

data class DrivePageResult(val files: List<DriveFile>, val nextPageToken: String?)
