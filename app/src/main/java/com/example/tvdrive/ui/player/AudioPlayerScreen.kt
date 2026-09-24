package com.example.tvdrive.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
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
import androidx.media3.ui.PlayerControlView
import coil.compose.AsyncImage
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.local.PlaybackPosition
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.GlassButton
import kotlinx.coroutines.launch

/**
 * TV Audio Player built on official Google Jetpack Media3 ExoPlayer.
 * Features:
 * - Jetpack Media3 PlayerControlView (native TV D-Pad focus & controls)
 * - Auto-refreshed OAuth Bearer tokens on HTTP 401
 * - Universal audio codec extractors (MP3, M4A, FLAC, WAV, AAC, OGG, OPUS)
 * - Ambient glassmorphic artwork & animated audio waveform
 * - Room resume playback position
 */
@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerScreen(
    streamUrl: String,
    fileId: String,
    title: String,
    albumArtUrl: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }

    // Waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val bars = (0..7).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(400 + i * 80, easing = FastOutSlowInEasing), RepeatMode.Reverse
            ),
            label = "bar_$i"
        )
    }

    // Build OkHttp data source with token auto-refresh on 401
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
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(6_000, 20_000, 1_500, 3_000)
                    .build()
            )
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
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: "Audio playback error (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(fileId) {
        val pos = container.database.playbackPositionDao().get(fileId)
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        if (pos != null && pos.positionMs > 0) exoPlayer.seekTo(pos.positionMs)
        exoPlayer.playWhenReady = true
    }

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

    AmbientGlowBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                // Album art or music note
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtUrl != null) {
                        AsyncImage(
                            model = albumArtUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Audiotrack,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Waveform visualizer
                if (isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bars.forEach { anim ->
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((32 * anim.value).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF2563EB))
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(32.dp))
                }

                // Jetpack Media3 Native PlayerControlView
                AndroidView(
                    factory = { ctx ->
                        PlayerControlView(ctx).apply {
                            player = exoPlayer
                            setShowTimeoutMs(0)  // Always show controls on audio screen
                            setShowFastForwardButton(true)
                            setShowRewindButton(true)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            post {
                                requestFocus()
                            }
                        }
                    },
                    modifier = Modifier.wrapContentSize()
                )

                // Close / Back Button
                GlassButton(
                    text = "Close Player",
                    iconVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack
                )
            }

            // Error Overlay if any
            if (playerError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.width(420.dp).padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Audio Playback Error",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = playerError ?: "Unable to stream this audio track.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
}
