package com.example.tvdrive.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tvdrive.theme.GoogleBlue
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-contrast, sharp QR code generator for TV screens.
 * Generates the QR bitmap off the main thread with ZXing.
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
                val bitMatrix = QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    512,
                    512,
                    hints
                )
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
            } catch (e: Exception) {
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
