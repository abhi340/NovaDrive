package com.example.tvdrive.data.repository

import com.example.tvdrive.data.model.PhotosAlbum
import com.example.tvdrive.data.model.PhotosMediaItem
import com.example.tvdrive.data.remote.PhotosRestClient

class PhotosRepository(private val client: PhotosRestClient) {

    suspend fun listAlbums(): Result<List<PhotosAlbum>> =
        runCatching { client.listAlbums().items }

    suspend fun listAllPhotos(pageToken: String? = null): Result<Pair<List<PhotosMediaItem>, String?>> =
        runCatching { client.listAllPhotos(pageToken).let { it.items to it.nextPageToken } }

    suspend fun listMediaInAlbum(albumId: String, pageToken: String? = null): Result<Pair<List<PhotosMediaItem>, String?>> =
        runCatching { client.listMediaInAlbum(albumId, pageToken).let { it.items to it.nextPageToken } }

    suspend fun searchMedia(query: String): Result<List<PhotosMediaItem>> =
        runCatching { client.searchMediaItems(query).items }

    /** Get a thumbnail URL sized for TV grid cards (320×180) */
    fun thumbnailUrl(baseUrl: String) = client.imageUrl(baseUrl, 320, 180)

    /** Get a full-res URL for the image viewer (TV resolution = 1920×1080 max) */
    fun fullResUrl(baseUrl: String) = client.imageUrl(baseUrl, 1920, 1080)
}
