package com.example.tvdrive

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tvdrive.auth.AuthManager
import com.example.tvdrive.auth.AuthState
import com.example.tvdrive.ui.auth.SignInScreen
import com.example.tvdrive.ui.drive.DriveBrowserScreen
import com.example.tvdrive.ui.home.HomeScreen
import com.example.tvdrive.ui.photos.AlbumDetailScreen
import com.example.tvdrive.ui.photos.AlbumsScreen
import com.example.tvdrive.ui.player.AudioPlayerScreen
import com.example.tvdrive.ui.player.VideoPlayerScreen
import com.example.tvdrive.ui.search.SearchScreen
import com.example.tvdrive.ui.settings.SettingsScreen
import com.example.tvdrive.ui.slideshow.SlideshowScreen
import com.example.tvdrive.ui.viewer.ImageViewerScreen
import com.example.tvdrive.ui.viewer.PdfViewerScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab { DRIVE, PHOTOS }

sealed class Screen {
    data object SignIn : Screen()
    data class Home(val selectedTab: HomeTab = HomeTab.DRIVE) : Screen()
    data class DriveBrowser(val folderId: String = "root", val folderName: String = "My Drive") : Screen()
    data object Albums : Screen()
    data class AlbumDetail(val albumId: String, val albumTitle: String) : Screen()
    data class ImageViewer(val imageUrls: List<String>, val startIndex: Int = 0, val title: String = "") : Screen()
    data class VideoPlayer(val streamUrl: String, val fileId: String, val title: String) : Screen()
    data class AudioPlayer(val streamUrl: String, val fileId: String, val title: String, val albumArtUrl: String? = null) : Screen()
    data class PdfViewer(val fileId: String, val title: String) : Screen()
    data object Search : Screen()
    data class Slideshow(val albumId: String? = null, val initialUrls: List<String> = emptyList()) : Screen()
    data object Settings : Screen()
}

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided.")
}

class AppViewModel(val authManager: AuthManager) : ViewModel() {
    private val _backStack = MutableStateFlow<List<Screen>>(emptyList())
    val backStack: StateFlow<List<Screen>> = _backStack

    val currentScreen: StateFlow<Screen?> = _backStack
        .map { it.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val authState = authManager.authState

    init {
        viewModelScope.launch {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {}
                    is AuthState.SignedOut, is AuthState.Error -> {
                        _backStack.value = listOf(Screen.SignIn)
                    }
                    is AuthState.SignedIn -> {
                        if (_backStack.value.isEmpty() || _backStack.value.last() == Screen.SignIn) {
                            _backStack.value = listOf(Screen.Home())
                        }
                    }
                }
            }
        }
    }

    fun navigate(screen: Screen) {
        _backStack.value = _backStack.value + screen
    }

    fun back(): Boolean {
        if (_backStack.value.size <= 1) return false
        _backStack.value = _backStack.value.dropLast(1)
        return true
    }

    fun navigateRoot(screen: Screen) {
        _backStack.value = listOf(screen)
    }

    fun replace(screen: Screen) {
        val stack = _backStack.value.toMutableList()
        if (stack.isNotEmpty()) stack[stack.lastIndex] = screen
        else stack.add(screen)
        _backStack.value = stack
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch { authManager.handleSignInResult(data) }
    }

    fun signOut() {
        viewModelScope.launch { authManager.signOut() }
    }

    class Factory(private val authManager: AuthManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(authManager) as T
    }
}


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
                onOpenSettings = { viewModel.navigate(Screen.Settings) }
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

            is Screen.Albums -> AlbumsScreen(
                onOpenAlbum = { id, title -> viewModel.navigate(Screen.AlbumDetail(id, title)) },
                onViewImage = { urls, idx, title -> viewModel.navigate(Screen.ImageViewer(urls, idx, title)) },
                onPlayVideo = { url, id, title -> viewModel.navigate(Screen.VideoPlayer(url, id, title)) },
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

            is Screen.Settings -> SettingsScreen(
                onSignOut = { viewModel.signOut() },
                onBack = { viewModel.back() }
            )
        }
    }
}
