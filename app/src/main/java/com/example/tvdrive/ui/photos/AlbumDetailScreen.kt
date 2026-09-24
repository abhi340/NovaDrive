package com.example.tvdrive.ui.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.model.PhotosMediaItem
import com.example.tvdrive.data.repository.PhotosRepository
import com.example.tvdrive.theme.*
import com.example.tvdrive.ui.components.TvFocusableItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class AlbumDetailUiState {
    object Loading : AlbumDetailUiState()
    data class Success(val items: List<PhotosMediaItem>) : AlbumDetailUiState()
    data class Error(val message: String) : AlbumDetailUiState()
}

class AlbumDetailViewModel(
    private val repository: PhotosRepository,
    val albumId: String
) : ViewModel() {
    private val _state = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val state: StateFlow<AlbumDetailUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = AlbumDetailUiState.Loading
            repository.listMediaInAlbum(albumId)
                .onSuccess { (items, _) -> _state.value = AlbumDetailUiState.Success(items) }
                .onFailure { _state.value = AlbumDetailUiState.Error(it.message ?: "Failed to load media") }
        }
    }

    fun thumbnailUrl(baseUrl: String) = repository.thumbnailUrl(baseUrl)
    fun fullResUrl(baseUrl: String) = repository.fullResUrl(baseUrl)

    class Factory(private val repo: PhotosRepository, private val albumId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AlbumDetailViewModel(repo, albumId) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AlbumDetailScreen(
    albumId: String,
    albumTitle: String,
    onViewImage: (urls: List<String>, startIdx: Int) -> Unit,
    onPlayVideo: (url: String, id: String, title: String) -> Unit,
    onSlideshow: () -> Unit,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val vm: AlbumDetailViewModel = viewModel(
        key = albumId,
        factory = AlbumDetailViewModel.Factory(container.photosRepository, albumId)
    )
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 32.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            com.example.tvdrive.ui.components.GlassButton(
                text = "Back",
                iconVector = androidx.compose.material.icons.Icons.Rounded.ArrowBack,
                onClick = onBack
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFECFDF5), androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = albumTitle,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            com.example.tvdrive.ui.components.GlassButton(
                text = "Slideshow",
                iconVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                isPrimary = true,
                onClick = onSlideshow
            )
        }

        when (val s = state) {
            is AlbumDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF059669))
            }
            is AlbumDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = s.message,
                        color = Color(0xFF475569),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    com.example.tvdrive.ui.components.GlassButton(
                        text = "Retry",
                        isPrimary = true,
                        onClick = { vm.load() }
                    )
                }
            }
            is AlbumDetailUiState.Success -> {
                // Precompute full-res URL list for image viewer navigation
                val imageUrls = remember(s.items) {
                    s.items.filter { !it.isVideo }.map { vm.fullResUrl(it.baseUrl) }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(s.items, key = { it.id }) { item ->
                        MediaItemCard(
                            item = item,
                            thumbnailUrl = vm.thumbnailUrl(item.baseUrl),
                            onClick = {
                                if (item.isVideo) {
                                    onPlayVideo(vm.fullResUrl(item.baseUrl), item.id, item.filename)
                                } else {
                                    val idx = imageUrls.indexOf(vm.fullResUrl(item.baseUrl)).coerceAtLeast(0)
                                    onViewImage(imageUrls, idx)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaItemCard(item: PhotosMediaItem, thumbnailUrl: String, onClick: () -> Unit) {
    TvFocusableItem(onClick = onClick, modifier = Modifier.size(200.dp, 130.dp), cornerRadius = 14.dp) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (focused) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = item.filename,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (item.isVideo) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.PlayCircle,
                        contentDescription = "Video",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}
