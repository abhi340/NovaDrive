package com.example.tvdrive.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import com.example.tvdrive.theme.GoogleBlue


/**
 * Reusable TV-focusable wrapper with scale & outline on focus.
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
                } else false
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        content = { content(focused) }
    )
}

/**
 * Clean, modern light background for Android TV.
 */
@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8FAFC), Color(0xFFEDF2F7))
                )
            ),
        content = content
    )
}

/**
 * Modern TV Surface Card with clean, crisp borders.
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0xFFFFFFFF),
    borderColor: Color = Color(0xFFE2E8F0),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

/**
 * TV D-pad focusable button with vector icon.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconVector: ImageVector? = null,
    icon: String? = null,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = tween(120),
        label = "tv_btn_scale"
    )

    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = when {
        isFocused && isPrimary -> Color(0xFF1D4ED8)
        isFocused && !isPrimary -> Color(0xFFEFF6FF)
        !isFocused && isPrimary -> Color(0xFF2563EB)
        else -> Color(0xFFFFFFFF)
    }

    val contentColor = when {
        isFocused && isPrimary -> Color.White
        isFocused && !isPrimary -> Color(0xFF1D4ED8)
        !isFocused && isPrimary -> Color.White
        else -> Color(0xFF334155)
    }

    val borderColor = when {
        isFocused -> Color(0xFF2563EB)
        isPrimary -> Color(0xFF1D4ED8)
        else -> Color(0xFFCBD5E1)
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(backgroundColor)
            .border(if (isFocused) 2.5.dp else 1.dp, borderColor, shape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if ((keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    if (keyEvent.type == KeyEventType.KeyDown) onClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            } else if (icon != null) {
                Text(text = icon, fontSize = 16.sp)
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Modern TV Grid Card for Drive files and folders.
 * Features:
 * - Distinct folder UI
 * - Visual photo thumbnail preview before opening (like Windows Explorer)
 * - Video thumbnail with prominent Play Icon overlay
 * - Distinct PDF emblem card
 */
@Composable
fun FileCard(
    file: DriveFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    streamUrl: String? = null
) {
    TvFocusableItem(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(168.dp),
        cornerRadius = 14.dp
    ) { focused ->
        val cardBg = if (focused) Color(0xFFEFF6FF) else Color(0xFFFFFFFF)
        val borderCol = if (focused) Color(0xFF2563EB) else Color(0xFFE2E8F0)
        val borderWidth = if (focused) 2.5.dp else 1.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg, RoundedCornerShape(14.dp))
                .border(borderWidth, borderCol, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            // Upper Preview Area (Fixed 112.dp so items never collapse)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    file.isFolder -> {
                        // Folder UI: prominent blue folder tile
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    file.isImage -> {
                        // Image Thumbnail Preview
                        val thumb = file.thumbnailLink ?: streamUrl
                        if (!thumb.isNullOrBlank()) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    file.isVideo -> {
                        // Video Thumbnail Preview with Play Icon Overlay
                        val thumb = file.thumbnailLink ?: streamUrl
                        if (!thumb.isNullOrBlank()) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Video Play Icon Overlay (Center Glass Badge)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Top-right "VIDEO" tag
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VIDEO",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    file.isPdf -> {
                        // PDF Preview Card
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFEF2F2)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PDF",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    file.isAudio -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFAF5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = file.fileCategory.iconVector,
                                contentDescription = null,
                                tint = file.fileCategory.iconColor,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // File Name
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (focused) Color(0xFF2563EB) else Color(0xFF0F172A),
                fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle Description (Folder, Video, Photo, PDF, etc.)
            val typeDesc = when {
                file.isFolder -> "Folder"
                file.isVideo  -> "Video"
                file.isImage  -> "Photo"
                file.isPdf    -> "PDF Document"
                file.isAudio  -> "Audio"
                file.isDocument -> "Document"
                else -> file.name.substringAfterLast('.', "").uppercase().ifEmpty { "File" }
            }
            Text(
                text = typeDesc,
                color = if (focused) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

val FileCategory.iconVector: ImageVector get() = when (this) {
    FileCategory.FOLDER      -> Icons.Rounded.Folder
    FileCategory.VIDEO       -> Icons.Rounded.PlayCircle
    FileCategory.AUDIO       -> Icons.Rounded.Audiotrack
    FileCategory.IMAGE       -> Icons.Rounded.Image
    FileCategory.PDF         -> Icons.Rounded.PictureAsPdf
    FileCategory.DOCUMENT    -> Icons.AutoMirrored.Rounded.Article
    FileCategory.UNSUPPORTED -> Icons.AutoMirrored.Rounded.InsertDriveFile
}

val FileCategory.iconColor: Color get() = when (this) {
    FileCategory.FOLDER      -> Color(0xFF2563EB)
    FileCategory.VIDEO       -> Color(0xFFDC2626)
    FileCategory.AUDIO       -> Color(0xFF7C3AED)
    FileCategory.IMAGE       -> Color(0xFF059669)
    FileCategory.PDF         -> Color(0xFFEA580C)
    FileCategory.DOCUMENT    -> Color(0xFF0284C7)
    FileCategory.UNSUPPORTED -> Color(0xFF94A3B8)
}

