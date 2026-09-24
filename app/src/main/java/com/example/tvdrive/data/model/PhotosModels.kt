package com.example.tvdrive.data.model

/** Domain models for Google Photos Library API */

data class PhotosAlbum(
    val id: String,
    val title: String,
    val coverPhotoBaseUrl: String? = null,
    val mediaItemsCount: String = "0"
)

data class PhotosMediaItem(
    val id: String,
    val baseUrl: String,
    val filename: String,
    val mimeType: String,
    val mediaMetadata: PhotosMetadata? = null
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

data class PhotosMetadata(
    val creationTime: String = "",
    val width: String = "0",
    val height: String = "0"
)
