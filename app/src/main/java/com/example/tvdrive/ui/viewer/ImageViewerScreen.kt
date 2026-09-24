package com.example.tvdrive.ui.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.size.Size
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.tvdrive.theme.*

/**
 * Full-screen image viewer with D-pad left/right navigation.
 *
 * Memory strategy:
 * - Full-res image loaded only for CURRENT index
 * - Only 1 image prefetched (next index) — not previous
 * - Coil's disk cache means re-visiting is nearly instant with 0 RAM cost
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
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Prefetch the NEXT image only (memory conscious)
    val nextIndex = (currentIndex + 1).coerceAtMost(imageUrls.lastIndex.coerceAtLeast(0))

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft  -> { if (currentIndex > 0) currentIndex--; true }
                        Key.DirectionRight -> { if (currentIndex < imageUrls.lastIndex) currentIndex++; true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (onStartSlideshow != null && imageUrls.isNotEmpty()) {
                                onStartSlideshow(imageUrls, currentIndex)
                                true
                            } else false
                        }
                        Key.Back, Key.Escape -> { onBack(); true }
                        else -> false
                    }
                } else false
            }
    ) {
        // Current image — full res (max 1920×1080)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrls.getOrNull(currentIndex))
                .size(1920, 1080)
                .build(),
            contentDescription = "Image ${currentIndex + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Prefetch next image silently (Coil loads into disk cache)
        if (imageUrls.size > 1) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls.getOrNull(nextIndex))
                    .size(1920, 1080)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(1.dp, 1.dp) // invisible, just triggers cache
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = if (imageUrls.size > 1) "Photo ${currentIndex + 1} of ${imageUrls.size}" else title.ifBlank { "Photo" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (onStartSlideshow != null && imageUrls.isNotEmpty()) {
                com.example.tvdrive.ui.components.GlassButton(
                    text = "Slideshow",
                    iconVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    onClick = { onStartSlideshow(imageUrls, currentIndex) }
                )
            }

            Text(
                "${currentIndex + 1} / ${imageUrls.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Navigation arrows
        if (currentIndex > 0) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Previous",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .size(48.dp)
            )
        }
        if (currentIndex < imageUrls.lastIndex) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Next",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .size(48.dp)
            )
        }
    }
}
