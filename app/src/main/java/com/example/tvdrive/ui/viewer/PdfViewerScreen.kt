package com.example.tvdrive.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.ui.components.GlassButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfViewerScreen(
    fileId: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val focusRequester = remember { FocusRequester() }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Renderer reference held across recompositions
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Render helper function
    fun renderPage(r: PdfRenderer, index: Int): Bitmap? {
        if (index < 0 || index >= r.pageCount) return null
        val page = r.openPage(index)
        // High quality rendering for TV (1080p fit)
        val targetWidth = 1920
        val targetHeight = (targetWidth * page.height) / page.width
        val bmp = Bitmap.createBitmap(targetWidth, targetHeight.coerceAtLeast(1080), Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bmp
    }

    // Load PDF file on launch
    LaunchedEffect(fileId) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val pdfFile = File(context.cacheDir, "pdf_cache_${fileId}.pdf")
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    val streamUrl = container.driveRepository.streamUrl(fileId)
                    val token = container.authManager.getAccessTokenSync()
                    val reqBuilder = Request.Builder().url(streamUrl)
                    if (token != null) {
                        reqBuilder.addHeader("Authorization", "Bearer $token")
                    }
                        val resp = container.okHttpClient.newCall(reqBuilder.build()).execute()
                        if (!resp.isSuccessful) {
                            throw Exception("Failed to download PDF: HTTP ${resp.code}")
                        }
                        resp.body?.byteStream()?.use { input ->
                            FileOutputStream(pdfFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(descriptor)
                pfd = descriptor
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount
                val bmp = renderPage(pdfRenderer, 0)
                currentBitmap = bmp
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to open PDF document"
                isLoading = false
            }
        }
    }

    // Clean up renderer on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                renderer?.close()
                pfd?.close()
                currentBitmap?.recycle()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionUp, Key.PageUp -> {
                            val r = renderer
                            if (r != null && currentPageIndex > 0) {
                                currentPageIndex--
                                currentBitmap = renderPage(r, currentPageIndex)
                            }
                            true
                        }
                        Key.DirectionRight, Key.DirectionDown, Key.PageDown -> {
                            val r = renderer
                            if (r != null && currentPageIndex < pageCount - 1) {
                                currentPageIndex++
                                currentBitmap = renderPage(r, currentPageIndex)
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
        // PDF Page Bitmap Render
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${currentPageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 56.dp)
            )
        }

        // Loading indicator
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                    Text(
                        text = "Opening PDF Document...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error message
        if (errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    GlassButton(
                        text = "Go Back",
                        iconVector = Icons.Rounded.ArrowBack,
                        onClick = onBack
                    )
                }
            }
        }

        // ── TOP BAR OVERLAY ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassButton(
                    text = "Back",
                    iconVector = Icons.Rounded.ArrowBack,
                    onClick = onBack
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            if (pageCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Page ${currentPageIndex + 1} of $pageCount",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── BOTTOM CONTROLS HINT ─────────────────────────────────────────────
        if (pageCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Left: Previous Page",
                    color = if (currentPageIndex > 0) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Use Remote D-Pad to Turn Pages  •  Back to Exit",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = "Right: Next Page",
                    color = if (currentPageIndex < pageCount - 1) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
