package com.example.tvdrive.data.remote

import com.example.tvdrive.auth.AuthManager
import com.example.tvdrive.data.model.PhotosAlbum
import com.example.tvdrive.data.model.PhotosMediaItem
import com.example.tvdrive.data.model.PhotosMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

private const val PHOTOS_API = "https://photoslibrary.googleapis.com/v1"
private const val DRIVE_API = "https://www.googleapis.com/drive/v3"

class PhotosRestClient(
    private val authManager: AuthManager,
    private val okHttpClient: OkHttpClient
) {
    private suspend fun token(): String? = authManager.getAccessToken()

    suspend fun listAlbums(pageToken: String? = null): PhotosPageResult<PhotosAlbum> = withContext(Dispatchers.IO) {
        val tok = token() ?: run {
            android.util.Log.e("PhotosRestClient", "listAlbums: No token available")
            return@withContext PhotosPageResult(emptyList(), null)
        }

        // Query Google Photos Library API exclusively
        val photosUrl = buildString {
            append("$PHOTOS_API/albums?pageSize=50")
            if (pageToken != null) append("&pageToken=$pageToken")
        }
        val json = getJson(photosUrl, tok)
        val arr = json?.optJSONArray("albums")
        if (arr != null && arr.length() > 0) {
            val albums = (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                PhotosAlbum(
                    id = a.optString("id"),
                    title = a.optString("title"),
                    coverPhotoBaseUrl = a.optString("coverPhotoBaseUrl").ifEmpty { null },
                    mediaItemsCount = a.optString("mediaItemsCount", "0")
                )
            }
            return@withContext PhotosPageResult(albums, json.optString("nextPageToken").ifEmpty { null })
        }
        PhotosPageResult(emptyList(), null)
    }

    suspend fun listMediaInAlbum(albumId: String, pageToken: String? = null): PhotosPageResult<PhotosMediaItem> =
        withContext(Dispatchers.IO) {
            val tok = token() ?: run {
                android.util.Log.e("PhotosRestClient", "listMediaInAlbum: No token available")
                return@withContext PhotosPageResult(emptyList(), null)
            }

            // Query media items inside the specific Google Photos album
            val body = JSONObject().apply {
                put("albumId", albumId)
                put("pageSize", 50)
                if (pageToken != null) put("pageToken", pageToken)
            }
            val json = postJson("$PHOTOS_API/mediaItems:search", tok, body)
            val items = json?.let { parseMediaItems(it) } ?: emptyList()
            PhotosPageResult(items, json?.optString("nextPageToken")?.ifEmpty { null })
        }

    suspend fun listAllPhotos(pageToken: String? = null): PhotosPageResult<PhotosMediaItem> =
        withContext(Dispatchers.IO) {
            val tok = token() ?: run {
                android.util.Log.e("PhotosRestClient", "listAllPhotos: No token available")
                return@withContext PhotosPageResult(emptyList(), null)
            }

            // Query Google Photos Library API mediaItems
            val photosUrl = buildString {
                append("$PHOTOS_API/mediaItems?pageSize=50")
                if (pageToken != null) append("&pageToken=$pageToken")
            }
            val json = getJson(photosUrl, tok)
            val items = json?.let { parseMediaItems(it) } ?: emptyList()
            if (items.isNotEmpty()) {
                android.util.Log.d("PhotosRestClient", "Loaded ${items.size} items from Photos Library API")
            }
            PhotosPageResult(items, json?.optString("nextPageToken")?.ifEmpty { null })
        }

    suspend fun searchMediaItems(query: String, pageToken: String? = null): PhotosPageResult<PhotosMediaItem> =
        withContext(Dispatchers.IO) {
            val tok = token() ?: return@withContext PhotosPageResult(emptyList(), null)
            val body = JSONObject().apply {
                put("pageSize", 50)
                if (pageToken != null) put("pageToken", pageToken)
            }
            val json = postJson("$PHOTOS_API/mediaItems:search", tok, body)
            val items = json?.let { parseMediaItems(it) } ?: emptyList()
            val filtered = items.filter { it.filename.contains(query, ignoreCase = true) }
            PhotosPageResult(filtered, json?.optString("nextPageToken")?.ifEmpty { null })
        }

    /** Returns a URL for an image at the given pixel dimensions */
    fun imageUrl(baseUrl: String, widthPx: Int, heightPx: Int): String {
        if (baseUrl.isBlank()) return ""
        return if (baseUrl.contains("googleusercontent.com")) {
            val cleanBase = if (baseUrl.contains("=")) baseUrl.substringBeforeLast("=") else baseUrl
            "$cleanBase=w$widthPx-h$heightPx"
        } else {
            baseUrl
        }
    }

    private fun getJson(url: String, token: String): JSONObject? {
        return try {
            var tok = token
            var resp = okHttpClient.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $tok")
                    .build()
            ).execute()
            if (resp.code == 401) {
                android.util.Log.w("PhotosRestClient", "HTTP 401 for $url. Invalidate and refresh token...")
                val freshTok = authManager.invalidateAndRefresh()
                if (!freshTok.isNullOrBlank()) {
                    tok = freshTok
                    resp = okHttpClient.newCall(
                        Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $tok")
                            .build()
                    ).execute()
                }
            }
            val bodyString = resp.body?.string()
            if (!resp.isSuccessful) {
                android.util.Log.e("PhotosRestClient", "GET $url failed HTTP ${resp.code}: $bodyString")
                null
            } else {
                JSONObject(bodyString ?: return null)
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotosRestClient", "GET $url error", e)
            null
        }
    }

    private fun postJson(url: String, token: String, body: JSONObject): JSONObject? {
        return try {
            var tok = token
            var resp = okHttpClient.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $tok")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()
            if (resp.code == 401) {
                android.util.Log.w("PhotosRestClient", "HTTP 401 for $url. Invalidate and refresh token...")
                val freshTok = authManager.invalidateAndRefresh()
                if (!freshTok.isNullOrBlank()) {
                    tok = freshTok
                    resp = okHttpClient.newCall(
                        Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $tok")
                            .post(body.toString().toRequestBody("application/json".toMediaType()))
                            .build()
                    ).execute()
                }
            }
            val bodyString = resp.body?.string()
            if (!resp.isSuccessful) {
                android.util.Log.e("PhotosRestClient", "POST $url failed HTTP ${resp.code}: $bodyString")
                null
            } else {
                JSONObject(bodyString ?: return null)
            }
        } catch (e: Exception) {
            android.util.Log.e("PhotosRestClient", "POST $url error", e)
            null
        }
    }

    private fun parseMediaItems(json: JSONObject): List<PhotosMediaItem> {
        val arr = json.optJSONArray("mediaItems") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val meta = item.optJSONObject("mediaMetadata")
            PhotosMediaItem(
                id = item.optString("id"),
                baseUrl = item.optString("baseUrl"),
                filename = item.optString("filename"),
                mimeType = item.optString("mimeType"),
                mediaMetadata = meta?.let {
                    PhotosMetadata(
                        creationTime = it.optString("creationTime"),
                        width = it.optString("width"),
                        height = it.optString("height")
                    )
                }
            )
        }
    }
}

data class PhotosPageResult<T>(val items: List<T>, val nextPageToken: String?)
