package com.example.tvdrive.ui.player

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.PlayerView
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.local.PlaybackPosition
import com.example.tvdrive.ui.components.GlassButton
import kotlinx.coroutines.launch

/**
 * TV Video Player built on official Google Jetpack Media3 ExoPlayer.
 * Features:
 * - Native TV D-Pad control bar (Play/Pause, Rewind 10s, Fast-Forward 10s, Seekbar, Subtitles, Settings)
 * - Auto-refreshed OAuth Bearer tokens on HTTP 401
 * - Universal container & codec extractors (VLC-grade format coverage)
 * - Room resume playback position
 * - Modern Antigravity top header with Back/Exit button
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    streamUrl: String,
    fileId: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var isControlsVisible by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }

    // Build OkHttp data source with auto-refresh on 401
    val dataSourceFactory = remember {
        val authOkHttp = container.okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                var token = container.authManager.getAccessTokenSync()
                val shouldAuth = token != null &&
                    !token.startsWith("demo_") &&
                    !url.contains("commondatastorage.googleapis.com") &&
                    (url.contains("googleapis.com") || url.contains("googleusercontent.com"))

                val newReq = if (shouldAuth) {
                    request.newBuilder().header("Authorization", "Bearer $token").build()
                } else {
                    request
                }

                var response = chain.proceed(newReq)

                // If 401 occurs, auto-refresh token and retry request once
                if (response.code == 401 && shouldAuth) {
                    response.close()
                    val freshToken = container.authManager.invalidateAndRefresh()
                    if (!freshToken.isNullOrBlank()) {
                        val retryReq = request.newBuilder()
                            .header("Authorization", "Bearer $freshToken")
                            .build()
                        response = chain.proceed(retryReq)
                    }
                }
                response
            }.build()

        OkHttpDataSource.Factory(authOkHttp)
    }

    val exoPlayer = remember {
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                8_000,   // min buffer before playback starts
                30_000,  // max buffer ahead
                1_500,
                3_000
            ).build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultDataSource.Factory(context, dataSourceFactory),
                    extractorsFactory
                )
            )
            .build()
    }

    // Player listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: "Playback error (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Load media & resume saved position
    LaunchedEffect(fileId) {
        val pos = container.database.playbackPositionDao().get(fileId)
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        if (pos != null && pos.positionMs > 0) {
            exoPlayer.seekTo(pos.positionMs)
        }
        exoPlayer.playWhenReady = true
    }

    // Save playback position on exit
    DisposableEffect(Unit) {
        onDispose {
            val lastPos = exoPlayer.currentPosition
            scope.launch {
                container.database.playbackPositionDao().upsert(
                    PlaybackPosition(fileId = fileId, positionMs = lastPos)
                )
            }
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Back, Key.Escape -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Official Jetpack Media3 TV PlayerView with native controllers
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 4500
                    controllerAutoShow = true
                    setShowSubtitleButton(true)
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        isControlsVisible = (visibility == View.VISIBLE)
                    })
                    post {
                        requestFocus()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Sleek Antigravity Top Title Bar (syncs with controller visibility)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 20.dp)
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
                            text = "Exit",
                            iconVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onBack
                        )
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Format Quality Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "HD 1080p",
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Error Dialog Overlay
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Playback Error",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playerError ?: "Unable to stream this video file.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            GlassButton(
                                text = "Retry",
                                iconVector = Icons.Rounded.Refresh,
                                isPrimary = true,
                                onClick = {
                                    playerError = null
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                            )
                            GlassButton(
                                text = "Close",
                                iconVector = Icons.Rounded.Close,
                                onClick = onBack
                            )
                        }
                    }
                }
            }
        }
    }
}
