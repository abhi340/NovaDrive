package com.example.tvdrive.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * Clean TV Grid Card for a Drive file or folder.
 */
@Composable
fun FileCard(
    file: DriveFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableItem(
        onClick = onClick,
        modifier = modifier
    ) { focused ->
        val cardBg = if (focused) Color(0xFFEFF6FF) else Color(0xFFFFFFFF)
        val borderCol = if (focused) Color(0xFF2563EB) else Color(0xFFE2E8F0)
        val borderWidth = if (focused) 2.5.dp else 1.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg, RoundedCornerShape(14.dp))
                .border(borderWidth, borderCol, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnailLink != null && (file.isImage || file.isVideo)) {
                    AsyncImage(
                        model = file.thumbnailLink,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = file.fileCategory.iconVector,
                        contentDescription = null,
                        tint = file.fileCategory.iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (focused) Color(0xFF2563EB) else Color(0xFF0F172A),
                fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

/**
 * QR Code Generator for TV sign-in.
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    backgroundColor: Int = AndroidColor.WHITE,
    foregroundColor: Int = AndroidColor.BLACK
) {
    var qrBitmap by remember(content) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(content) {
        if (content.isBlank()) return@LaunchedEffect
        qrBitmap = withContext(Dispatchers.Default) {
            try {
                val hints = mapOf(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8"
                )
                val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    val offset = y * width
                    for (x in 0 until width) {
                        pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                    }
                }
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    setPixels(pixels, 0, width, 0, 0, width, height)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = qrBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR code for sign in",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                color = GoogleBlue,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
