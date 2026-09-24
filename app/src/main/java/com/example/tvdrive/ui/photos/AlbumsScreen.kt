package com.example.tvdrive.ui.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.model.PhotosAlbum
import com.example.tvdrive.data.model.PhotosMediaItem
import com.example.tvdrive.data.repository.PhotosRepository
import com.example.tvdrive.ui.components.GlassButton
import com.example.tvdrive.ui.components.TvFocusableItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

sealed class PhotosUiState {
    object Loading : PhotosUiState()
    data class Success(
        val photos: List<PhotosMediaItem>,
        val albums: List<PhotosAlbum>
    ) : PhotosUiState()
    data class Error(val message: String) : PhotosUiState()
}

enum class PhotosTab { RECENT, ALBUMS }

class AlbumsViewModel(private val repository: PhotosRepository) : ViewModel() {
    private val _state = MutableStateFlow<PhotosUiState>(PhotosUiState.Loading)
    val state: StateFlow<PhotosUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = PhotosUiState.Loading
            try {
                val photosResult = repository.listAllPhotos()
                val albumsResult = repository.listAlbums()

                val photos = photosResult.getOrNull()?.first ?: emptyList()
                val albums = albumsResult.getOrNull() ?: emptyList()

                if (photos.isEmpty() && albums.isEmpty() && photosResult.isFailure) {
                    _state.value = PhotosUiState.Error(
                        photosResult.exceptionOrNull()?.message ?: "Failed to load photos"
                    )
                } else {
                    _state.value = PhotosUiState.Success(photos, albums)
                }
            } catch (e: Exception) {
                _state.value = PhotosUiState.Error(e.message ?: "Failed to load photos")
            }
        }
    }

    fun thumbnailUrl(baseUrl: String) = repository.thumbnailUrl(baseUrl)
    fun fullResUrl(baseUrl: String) = repository.fullResUrl(baseUrl)

    class Factory(private val repo: PhotosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AlbumsViewModel(repo) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun AlbumsScreen(
    onOpenAlbum: (albumId: String, title: String) -> Unit,
    onViewImage: (urls: List<String>, idx: Int, title: String) -> Unit,
    onStartSlideshow: (urls: List<String>) -> Unit,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val vm: AlbumsViewModel = viewModel(factory = AlbumsViewModel.Factory(container.photosRepository))
    val state by vm.state.collectAsState()

    var selectedTab by remember { mutableStateOf(PhotosTab.RECENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 32.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassButton(
                text = "Back",
                iconVector = Icons.Rounded.ArrowBack,
                onClick = onBack
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Google Photos",
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            // Section Tabs: Recent Photos vs Albums
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabChip(
                    text = "Recent Photos",
                    icon = Icons.Rounded.Photo,
                    isSelected = selectedTab == PhotosTab.RECENT,
                    onClick = { selectedTab = PhotosTab.RECENT }
                )
                TabChip(
                    text = "Albums",
                    icon = Icons.Rounded.FolderSpecial,
                    isSelected = selectedTab == PhotosTab.ALBUMS,
                    onClick = { selectedTab = PhotosTab.ALBUMS }
                )
            }

            Spacer(Modifier.weight(1f))

            // Action: Start Slideshow
            val currentPhotoUrls = remember(state) {
                (state as? PhotosUiState.Success)?.photos?.filter { !it.isVideo }?.map { vm.fullResUrl(it.baseUrl) } ?: emptyList()
            }
            if (currentPhotoUrls.isNotEmpty()) {
                GlassButton(
                    text = "Start Slideshow",
                    iconVector = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    onClick = { onStartSlideshow(currentPhotoUrls) }
                )
            }

            GlassButton(
                text = "Refresh",
                iconVector = Icons.Rounded.Refresh,
                onClick = { vm.load() }
            )
        }

        // ── Main Content ──────────────────────────────────────────────────────
        when (val s = state) {
            is PhotosUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF059669))
            }
            is PhotosUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
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
                    GlassButton(
                        text = "Retry",
                        isPrimary = true,
                        onClick = { vm.load() }
                    )
                }
            }
            is PhotosUiState.Success -> {
                if (selectedTab == PhotosTab.RECENT) {
                    // ── RECENT PHOTOS STREAM ──────────────────────────────────
                    if (s.photos.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Photo,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    text = "No photos found in your library",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        val photoUrls = remember(s.photos) {
                            s.photos.filter { !it.isVideo }.map { vm.fullResUrl(it.baseUrl) }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 220.dp),
                            contentPadding = PaddingValues(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(s.photos, key = { _, item -> item.id }) { index, photo ->
                                PhotoItemCard(
                                    item = photo,
                                    thumbnailUrl = vm.thumbnailUrl(photo.baseUrl),
                                    onClick = {
                                        val fullUrl = vm.fullResUrl(photo.baseUrl)
                                        val idx = photoUrls.indexOf(fullUrl).coerceAtLeast(0)
                                        onViewImage(photoUrls, idx, photo.filename)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // ── ALBUMS VIEW ───────────────────────────────────────────
                    if (s.albums.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderSpecial,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    text = "No albums found",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 240.dp),
                            contentPadding = PaddingValues(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(s.albums, key = { it.id }) { album ->
                                AlbumCard(
                                    album = album,
                                    thumbnailUrl = album.coverPhotoBaseUrl?.let { vm.thumbnailUrl(it) },
                                    onClick = { onOpenAlbum(album.id, album.title) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> Color(0xFF059669)
                    isFocused -> Color(0xFFE2E8F0)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color(0xFF059669) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF475569),
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PhotoItemCard(
    item: PhotosMediaItem,
    thumbnailUrl: String,
    onClick: () -> Unit
) {
    TvFocusableItem(onClick = onClick, modifier = Modifier.size(220.dp, 140.dp), cornerRadius = 14.dp) { focused ->
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

            // Gradient shadow on bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(48.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            Text(
                text = item.filename,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun AlbumCard(album: PhotosAlbum, thumbnailUrl: String?, onClick: () -> Unit) {
    TvFocusableItem(onClick = onClick, modifier = Modifier.size(240.dp, 150.dp), cornerRadius = 14.dp) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (focused) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(56.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (thumbnailUrl != null) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${album.mediaItemsCount} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (thumbnailUrl != null) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                )
            }
        }
    }
}
