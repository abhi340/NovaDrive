package com.example.tvdrive.data.model

enum class FileCategory { FOLDER, VIDEO, AUDIO, IMAGE, PDF, DOCUMENT, UNSUPPORTED }

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long = 0L,
    val modifiedTime: String = "",
    val thumbnailLink: String? = null,
    val webContentLink: String? = null,
    val parents: List<String> = emptyList()
) {
    private val ext: String get() = name.substringAfterLast('.', "").lowercase()

    val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"

    val isVideo: Boolean get() = mimeType.startsWith("video/") ||
        mimeType == "application/x-matroska" ||
        mimeType == "application/x-msvideo" ||
        ext in setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "ts", "m2ts", "wmv", "3gp", "m4v", "vob", "divx", "ogv", "rm", "rmvb", "asf", "f4v")

    val isAudio: Boolean get() = mimeType.startsWith("audio/") ||
        mimeType == "application/ogg" ||
        mimeType == "application/x-flac" ||
        ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma", "mid", "alac", "aiff", "mka", "amr")

    val isImage: Boolean get() = mimeType.startsWith("image/") ||
        ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "svg")

    val isPdf: Boolean get() = mimeType == "application/pdf" || ext == "pdf"

    val isDocument: Boolean get() = (mimeType.startsWith("application/vnd.google-apps") && !isFolder) ||
        ext in setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt", "ods", "odp")

    val fileCategory: FileCategory get() = when {
        isFolder   -> FileCategory.FOLDER
        isVideo    -> FileCategory.VIDEO
        isAudio    -> FileCategory.AUDIO
        isImage    -> FileCategory.IMAGE
        isPdf      -> FileCategory.PDF
        isDocument -> FileCategory.DOCUMENT
        else       -> FileCategory.UNSUPPORTED
    }
}

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
