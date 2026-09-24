package com.example.tvdrive.ui.slideshow

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tvdrive.LocalAppContainer
import kotlinx.coroutines.delay

private const val DEFAULT_SLIDE_INTERVAL_MS = 6_000L  // 6 seconds per slide

@Composable
fun SlideshowScreen(
    albumId: String? = null,
    initialUrls: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var imageUrls by remember { mutableStateOf(initialUrls) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(initialUrls.isEmpty()) }
    var showPauseNotice by remember { mutableStateOf(false) }

    // Load URLs if not provided directly
    LaunchedEffect(albumId, initialUrls) {
        if (initialUrls.isNotEmpty()) {
            imageUrls = initialUrls
            loading = false
            return@LaunchedEffect
        }
        loading = true
        if (albumId != null) {
            container.photosRepository.listMediaInAlbum(albumId)
                .onSuccess { (items, _) ->
                    imageUrls = items.filter { !it.isVideo }
                        .map { container.photosRepository.fullResUrl(it.baseUrl) }
                }
        } else {
            // Load from recent photos
            container.photosRepository.listAllPhotos()
                .onSuccess { (items, _) ->
                    imageUrls = items.filter { !it.isVideo }
                        .map { container.photosRepository.fullResUrl(it.baseUrl) }
                }
        }
        loading = false
    }

    // Auto-advance loop when not paused
    LaunchedEffect(imageUrls, isPaused) {
        if (imageUrls.isEmpty() || isPaused) return@LaunchedEffect
        while (true) {
            delay(DEFAULT_SLIDE_INTERVAL_MS)
            if (!isPaused && imageUrls.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % imageUrls.size
            }
        }
    }

    // Pause notice transient auto-hide
    LaunchedEffect(showPauseNotice) {
        if (showPauseNotice) {
            delay(1500)
            showPauseNotice = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Ken Burns slow pan & zoom effect
    val transition = rememberInfiniteTransition(label = "kenburns")
    val panX by transition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(DEFAULT_SLIDE_INTERVAL_MS.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panX"
    )
    val zoom by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(DEFAULT_SLIDE_INTERVAL_MS.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> {
                            isPaused = !isPaused
                            showPauseNotice = true
                            true
                        }
                        Key.DirectionLeft, Key.MediaRewind -> {
                            if (imageUrls.isNotEmpty()) {
                                currentIndex = (currentIndex - 1 + imageUrls.size) % imageUrls.size
                            }
                            true
                        }
                        Key.DirectionRight, Key.MediaFastForward -> {
                            if (imageUrls.isNotEmpty()) {
                                currentIndex = (currentIndex + 1) % imageUrls.size
                            }
                            true
                        }
                        Key.Back, Key.Escape -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                    Text("Starting Slideshow...", color = Color.White, fontSize = 16.sp)
                }
            }
        } else if (imageUrls.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text("No photos available for slideshow", color = Color.White, fontSize = 18.sp)
                }
            }
        } else {
            // Current Slide Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls.getOrNull(currentIndex))
                    .size(1920, 1080)
                    .crossfade(600)
                    .build(),
                contentDescription = "Slideshow photo ${currentIndex + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = panX
                        scaleX = zoom
                        scaleY = zoom
                    }
            )

            // Next Image Prefetch (1 ahead)
            if (imageUrls.size > 1) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrls.getOrNull((currentIndex + 1) % imageUrls.size))
                        .size(1920, 1080)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(1.dp, 1.dp)
                )
            }

            // Top Status Bar: Pause / Play state and counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 20.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("PAUSED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${imageUrls.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Temporary Pause/Play Center Banner
            if (showPauseNotice) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (isPaused) "Slideshow Paused" else "Slideshow Playing",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Navigation Hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "OK: Pause/Resume  •  Left / Right: Skip  •  Back: Exit",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
