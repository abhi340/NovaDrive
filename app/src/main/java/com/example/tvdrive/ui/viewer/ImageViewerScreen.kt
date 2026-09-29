package com.example.tvdrive.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import com.example.tvdrive.ui.components.GlassButton
import kotlinx.coroutines.delay

/**
 * 10-foot Cinema Image Viewer for Android TV.
 * - Ambient backdrop fill (eliminates harsh black pillarboxes on portrait/4:3 photos)
 * - Auto-hiding OSD top bar & navigation indicators on 3.5s inactivity
 * - D-pad Left/Right navigation with smooth crossfade
 * - Single-image memory footprint with 1-ahead cache prefetch
 */
@Composable
fun ImageViewerScreen(
    imageUrls: List<String>,
    startIndex: Int,
    title: String,
    onStartSlideshow: ((List<String>, Int) -> Unit)? = null,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, imageUrls.lastIndex.coerceAtLeast(0))) }
    var showControls by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val currentUrl = imageUrls.getOrNull(currentIndex)
    val nextIndex = (currentIndex + 1).coerceAtMost(imageUrls.lastIndex.coerceAtLeast(0))

    // Auto-hide controls after 3.5s
    LaunchedEffect(showControls, currentIndex) {
        if (showControls) {
            delay(3500)
            showControls = false
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    showControls = true
                    when (event.key) {
                        Key.DirectionLeft  -> { if (currentIndex > 0) currentIndex--; true }
                        Key.DirectionRight -> { if (currentIndex < imageUrls.lastIndex) currentIndex++; true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (onStartSlideshow != null && imageUrls.isNotEmpty()) {
                                onStartSlideshow(imageUrls, currentIndex)
                            } else {
                                showControls = !showControls
                            }
                            true
                        }
                        Key.Back, Key.Escape -> { onBack(); true }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Ambient Background Backdrop (softly fills letterbox areas with photo tones)
        if (currentUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.28f }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // 2. Foreground High-Fidelity Photo (Aspect Ratio Preserved)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentUrl)
                .size(1920, 1080)
                .crossfade(300)
                .build(),
            contentDescription = "Image ${currentIndex + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Silent 1-Ahead Image Prefetch into Disk Cache
        if (imageUrls.size > 1) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls.getOrNull(nextIndex))
                    .size(1920, 1080)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(1.dp, 1.dp)
            )
        }

        // 4. Sleek Top Bar Overlay (Auto-Hides)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                    .padding(horizontal = 32.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        GlassButton(
                            text = "Back",
                            iconVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onBack
                        )
                        Text(
                            text = if (imageUrls.size > 1) "Photo ${currentIndex + 1} of ${imageUrls.size}" else title.ifBlank { "Photo" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onStartSlideshow != null && imageUrls.isNotEmpty()) {
                            GlassButton(
                                text = "Start Slideshow",
                                iconVector = Icons.Rounded.PlayArrow,
                                isPrimary = true,
                                onClick = { onStartSlideshow(imageUrls, currentIndex) }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${currentIndex + 1} / ${imageUrls.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Subtle Direction Indicators (Auto-Hides)
        AnimatedVisibility(
            visible = showControls && currentIndex > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showControls && currentIndex < imageUrls.lastIndex,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
