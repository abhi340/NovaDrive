package com.example.tvdrive

import androidx.compose.runtime.compositionLocalOf

/**
 * Sealed class representing every screen in the app.
 * Navigation is a simple List<Screen> backstack held in AppViewModel.
 * Only the LAST item in the stack is composed — no retained Composable state.
 */
sealed class Screen {
    data object SignIn : Screen()

    /** Combined home: Drive tab + Photos tab */
    data class Home(val selectedTab: HomeTab = HomeTab.DRIVE) : Screen()

    /** Drive folder browser — each push = one folder deeper */
    data class DriveBrowser(
        val folderId: String = "root",
        val folderName: String = "My Drive"
    ) : Screen()

    /** Photos albums list */
    data object Albums : Screen()

    /** Single album's media grid */
    data class AlbumDetail(val albumId: String, val albumTitle: String) : Screen()

    /** Full-screen image viewer — pass list of URLs + start index */
    data class ImageViewer(
        val imageUrls: List<String>,
        val startIndex: Int = 0,
        val title: String = ""
    ) : Screen()

    /** ExoPlayer video player */
    data class VideoPlayer(
        val streamUrl: String,
        val fileId: String,
        val title: String
    ) : Screen()

    /** Audio now-playing */
    data class AudioPlayer(
        val streamUrl: String,
        val fileId: String,
        val title: String,
        val albumArtUrl: String? = null
    ) : Screen()

    /** PDF Document viewer */
    data class PdfViewer(
        val fileId: String,
        val title: String
    ) : Screen()

    /** Unified Drive + Photos search */
    data object Search : Screen()

    /** Slideshow / Screensaver — null albumId = use initialUrls or Drive images */
    data class Slideshow(
        val albumId: String? = null,
        val initialUrls: List<String> = emptyList()
    ) : Screen()

    /** Download manager */
    data object Downloads : Screen()

    /** App settings */
    data object Settings : Screen()
}

enum class HomeTab { DRIVE, PHOTOS }

/** CompositionLocal for AppContainer — avoids prop-drilling through every composable */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap your composables with CompositionLocalProvider.")
}
