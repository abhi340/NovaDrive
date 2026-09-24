package com.example.tvdrive.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tvdrive.data.model.DriveFile
import com.example.tvdrive.data.model.FileCategory
import com.example.tvdrive.theme.*

/**
 * Reusable TV-focusable wrapper.
 * On focus: scale up + high-contrast outline. Smooth 140ms animation.
 */
@Composable
fun TvFocusableItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(130),
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (focused) Color(0xFFEFF6FF) else Color.White)
            .border(
                width = if (focused) 2.5.dp else 1.dp,
                color = if (focused) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        content(focused)
    }
}

/**
 * File/folder card for Drive browser grid.
 * Thumbnail from Drive API, modern vector icon fallback for non-image files.
 * Fixed 200×130 dp — fits cleanly in grid on 1080p TV.
 */
@Composable
fun FileCard(
    file: DriveFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableItem(onClick = onClick, modifier = modifier.size(width = 200.dp, height = 130.dp), cornerRadius = 14.dp) { focused ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail or vector icon
            if (file.thumbnailLink != null) {
                AsyncImage(
                    model = file.thumbnailLink,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient scrim for text readability over thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(52.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
            } else {
                // Vector SVG Icon for non-image files
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (focused) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(file.fileCategory.iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = file.fileCategory.iconVector,
                            contentDescription = null,
                            tint = file.fileCategory.iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // File name at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = file.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (file.thumbnailLink != null) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

val FileCategory.iconVector: ImageVector get() = when (this) {
    FileCategory.FOLDER      -> Icons.Rounded.Folder
    FileCategory.IMAGE       -> Icons.Rounded.Image
    FileCategory.VIDEO       -> Icons.Rounded.PlayCircle
    FileCategory.AUDIO       -> Icons.Rounded.Audiotrack
    FileCategory.PDF         -> Icons.Rounded.Description
    FileCategory.DOCUMENT    -> Icons.Rounded.Article
    FileCategory.UNSUPPORTED -> Icons.Rounded.InsertDriveFile
}

val FileCategory.iconColor: Color get() = when (this) {
    FileCategory.FOLDER      -> Color(0xFF2563EB) // Blue
    FileCategory.IMAGE       -> Color(0xFF059669) // Emerald
    FileCategory.VIDEO       -> Color(0xFFDC2626) // Red
    FileCategory.AUDIO       -> Color(0xFF7C3AED) // Purple
    FileCategory.PDF         -> Color(0xFFEA580C) // Orange
    FileCategory.DOCUMENT    -> Color(0xFF0284C7) // Sky
    FileCategory.UNSUPPORTED -> Color(0xFF64748B) // Slate
}
