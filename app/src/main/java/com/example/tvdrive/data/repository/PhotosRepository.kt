package com.example.tvdrive.data.repository

import com.example.tvdrive.data.model.PhotosAlbum
import com.example.tvdrive.data.model.PhotosMediaItem
import com.example.tvdrive.data.remote.PhotosRestClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotosRepository(private val client: PhotosRestClient) {

    suspend fun listAlbums(): Result<List<PhotosAlbum>> = withContext(Dispatchers.IO) {
        try {
            val result = client.listAlbums()
            Result.success(result.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listAllPhotos(pageToken: String? = null): Result<Pair<List<PhotosMediaItem>, String?>> =
        withContext(Dispatchers.IO) {
            try {
                val result = client.listAllPhotos(pageToken)
                Result.success(result.items to result.nextPageToken)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listMediaInAlbum(albumId: String, pageToken: String? = null)
    : Result<Pair<List<PhotosMediaItem>, String?>> = withContext(Dispatchers.IO) {
        try {
            val result = client.listMediaInAlbum(albumId, pageToken)
            Result.success(result.items to result.nextPageToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMedia(query: String): Result<List<PhotosMediaItem>> = withContext(Dispatchers.IO) {
        try {
            Result.success(client.searchMediaItems(query).items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get a thumbnail URL sized for TV grid cards (320×180) */
    fun thumbnailUrl(baseUrl: String) = client.imageUrl(baseUrl, 320, 180)

    /** Get a full-res URL for the image viewer (TV resolution = 1920×1080 max) */
    fun fullResUrl(baseUrl: String) = client.imageUrl(baseUrl, 1920, 1080)
}
