package com.example.tvdrive.ui.player

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
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
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.tvdrive.AppContainer
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.local.PlaybackPosition
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.GlassButton
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
private fun createMedia3DataSource(container: AppContainer): OkHttpDataSource.Factory {
    val authOkHttp = container.okHttpClient.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            val token = container.authManager.getAccessTokenSync()
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
            if (response.code == 401 && shouldAuth) {
                response.close()
                val freshToken = container.authManager.invalidateAndRefresh()
                if (!freshToken.isNullOrBlank()) {
                    response = chain.proceed(
                        request.newBuilder().header("Authorization", "Bearer $freshToken").build()
                    )
                }
            }
            response
        }.build()

    return OkHttpDataSource.Factory(authOkHttp)
}

// ── Video Player ─────────────────────────────────────────────────────────────

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
    val dataSourceFactory = remember { createMedia3DataSource(container) }

    val exoPlayer = remember {
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 30_000, 1_500, 3_000)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(context, dataSourceFactory), extractorsFactory)
            )
            .build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: "Playback error (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    onBack(); true
                } else false
            }
    ) {
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
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        isControlsVisible = (visibility == View.VISIBLE)
                    })
                    post { requestFocus() }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
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
                        GlassButton(text = "Exit", iconVector = Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
                        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "HD 1080p", color = Color(0xFF93C5FD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (playerError != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.width(420.dp).padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
                        Text(text = "Playback Error", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = playerError ?: "Unable to stream this video file.", color = Color(0xFF94A3B8), fontSize = 13.sp, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassButton(text = "Retry", iconVector = Icons.Rounded.Refresh, isPrimary = true, onClick = {
                                playerError = null; exoPlayer.prepare(); exoPlayer.play()
                            })
                            GlassButton(text = "Close", iconVector = Icons.Rounded.Close, onClick = onBack)
                        }
                    }
                }
            }
        }
    }
}

// ── Audio Player ─────────────────────────────────────────────────────────────

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
    val dataSourceFactory = remember { createMedia3DataSource(container) }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val bars = (0..7).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(400 + i * 80, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar_$i"
        )
    }

    val exoPlayer = remember {
        val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(6_000, 20_000, 1_500, 3_000).build())
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, dataSourceFactory), extractorsFactory))
            .build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.localizedMessage ?: "Audio playback error (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
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
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                        onBack(); true
                    } else false
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier.size(140.dp).clip(CircleShape).background(Color.White).border(2.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArtUrl != null) {
                        AsyncImage(model = albumArtUrl, contentDescription = "Album Art", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(imageVector = Icons.Rounded.Audiotrack, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(56.dp))
                    }
                }

                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)

                if (isPlaying) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        bars.forEach { anim ->
                            Box(modifier = Modifier.width(6.dp).height((32 * anim.value).dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2563EB)))
                        }
                    }
                } else {
                    Spacer(Modifier.height(32.dp))
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerControlView(ctx).apply {
                            player = exoPlayer
                            setShowTimeoutMs(0)
                            setShowFastForwardButton(true)
                            setShowRewindButton(true)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            post { requestFocus() }
                        }
                    },
                    modifier = Modifier.wrapContentSize()
                )

                GlassButton(text = "Close Player", iconVector = Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
            }

            if (playerError != null) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.width(420.dp).padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(imageVector = Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(44.dp))
                            Text(text = "Audio Playback Error", color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = playerError ?: "Unable to stream this audio track.", color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GlassButton(text = "Retry", iconVector = Icons.Rounded.Refresh, isPrimary = true, onClick = {
                                    playerError = null; exoPlayer.prepare(); exoPlayer.play()
                                })
                                GlassButton(text = "Close", iconVector = Icons.Rounded.Close, onClick = onBack)
                            }
                        }
                    }
                }
            }
        }
    }
}
