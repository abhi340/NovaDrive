package com.example.tvdrive

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tvdrive.auth.AuthState
import com.example.tvdrive.ui.auth.SignInScreen
import com.example.tvdrive.ui.drive.DriveBrowserScreen
import com.example.tvdrive.ui.home.HomeScreen
import com.example.tvdrive.ui.photos.AlbumDetailScreen
import com.example.tvdrive.ui.player.AudioPlayerScreen
import com.example.tvdrive.ui.player.VideoPlayerScreen
import com.example.tvdrive.ui.search.SearchScreen
import com.example.tvdrive.ui.settings.SettingsScreen
import com.example.tvdrive.ui.slideshow.SlideshowScreen
import com.example.tvdrive.ui.viewer.ImageViewerScreen
import com.example.tvdrive.ui.viewer.PdfViewerScreen

/**
 * Root navigation composable.
 * Uses a simple [Crossfade] over a sealed-class [Screen] — no navigation library needed.
 *
 * Only the CURRENT screen is in composition at any time.
 * This is the primary low-RAM technique: previous screens are completely disposed.
 */
@Composable
fun AppNavHost(
    screen: Screen?,
    viewModel: AppViewModel,
    onStartSignIn: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    Crossfade(
        targetState = screen,
        animationSpec = tween(durationMillis = 250),
        label = "nav_crossfade"
    ) { currentScreen ->
        when (currentScreen) {
            null, Screen.SignIn -> SignInScreen(
                authState = authState,
                onStartSignIn = onStartSignIn
            )

            is Screen.Home -> HomeScreen(
                selectedTab = currentScreen.selectedTab,
                onTabChange = { tab -> viewModel.replace(Screen.Home(tab)) },
                onOpenFolder = { id, name -> viewModel.navigate(Screen.DriveBrowser(id, name)) },
                onOpenAlbums = { viewModel.navigate(Screen.Albums) },
                onOpenSearch = { viewModel.navigate(Screen.Search) },
                onOpenSettings = { viewModel.navigate(Screen.Settings) },
                onOpenDownloads = { viewModel.navigate(Screen.Downloads) }
            )

            is Screen.DriveBrowser -> DriveBrowserScreen(
                folderId = currentScreen.folderId,
                folderName = currentScreen.folderName,
                onOpenFolder = { id, name -> viewModel.navigate(Screen.DriveBrowser(id, name)) },
                onPlayVideo = { url, id, title -> viewModel.navigate(Screen.VideoPlayer(url, id, title)) },
                onPlayAudio = { url, id, title -> viewModel.navigate(Screen.AudioPlayer(url, id, title)) },
                onViewImage = { urls, idx, title -> viewModel.navigate(Screen.ImageViewer(urls, idx, title)) },
                onOpenPdf = { id, name -> viewModel.navigate(Screen.PdfViewer(id, name)) },
                onStartSlideshow = { urls -> viewModel.navigate(Screen.Slideshow(null, urls)) },
                onBack = { viewModel.back() }
            )

            is Screen.Albums -> com.example.tvdrive.ui.photos.AlbumsScreen(
                onOpenAlbum = { id, title -> viewModel.navigate(Screen.AlbumDetail(id, title)) },
                onViewImage = { urls, idx, title -> viewModel.navigate(Screen.ImageViewer(urls, idx, title)) },
                onStartSlideshow = { urls -> viewModel.navigate(Screen.Slideshow(null, urls)) },
                onBack = { viewModel.back() }
            )

            is Screen.AlbumDetail -> AlbumDetailScreen(
                albumId = currentScreen.albumId,
                albumTitle = currentScreen.albumTitle,
                onViewImage = { urls, idx -> viewModel.navigate(Screen.ImageViewer(urls, idx, currentScreen.albumTitle)) },
                onPlayVideo = { url, id, title -> viewModel.navigate(Screen.VideoPlayer(url, id, title)) },
                onSlideshow = { viewModel.navigate(Screen.Slideshow(currentScreen.albumId)) },
                onBack = { viewModel.back() }
            )

            is Screen.ImageViewer -> ImageViewerScreen(
                imageUrls = currentScreen.imageUrls,
                startIndex = currentScreen.startIndex,
                title = currentScreen.title,
                onStartSlideshow = { urls, startIdx ->
                    val reordered = if (startIdx in urls.indices) {
                        urls.subList(startIdx, urls.size) + urls.subList(0, startIdx)
                    } else urls
                    viewModel.navigate(Screen.Slideshow(null, reordered))
                },
                onBack = { viewModel.back() }
            )

            is Screen.PdfViewer -> PdfViewerScreen(
                fileId = currentScreen.fileId,
                title = currentScreen.title,
                onBack = { viewModel.back() }
            )

            is Screen.VideoPlayer -> VideoPlayerScreen(
                streamUrl = currentScreen.streamUrl,
                fileId = currentScreen.fileId,
                title = currentScreen.title,
                onBack = { viewModel.back() }
            )

            is Screen.AudioPlayer -> AudioPlayerScreen(
                streamUrl = currentScreen.streamUrl,
                fileId = currentScreen.fileId,
                title = currentScreen.title,
                albumArtUrl = currentScreen.albumArtUrl,
                onBack = { viewModel.back() }
            )

            is Screen.Search -> SearchScreen(
                onOpenFolder = { id, name -> viewModel.navigate(Screen.DriveBrowser(id, name)) },
                onPlayVideo = { url, id, title -> viewModel.navigate(Screen.VideoPlayer(url, id, title)) },
                onPlayAudio = { url, id, title -> viewModel.navigate(Screen.AudioPlayer(url, id, title)) },
                onViewImage = { urls, idx, title -> viewModel.navigate(Screen.ImageViewer(urls, idx, title)) },
                onOpenPdf = { id, name -> viewModel.navigate(Screen.PdfViewer(id, name)) },
                onBack = { viewModel.back() }
            )

            is Screen.Slideshow -> SlideshowScreen(
                albumId = currentScreen.albumId,
                initialUrls = currentScreen.initialUrls,
                onBack = { viewModel.back() }
            )

            is Screen.Downloads -> com.example.tvdrive.ui.downloads.DownloadsScreen(
                onBack = { viewModel.back() }
            )

            is Screen.Settings -> SettingsScreen(
                onSignOut = { viewModel.signOut() },
                onOpenDownloads = { viewModel.navigate(Screen.Downloads) },
                onBack = { viewModel.back() }
            )
        }
    }
}
