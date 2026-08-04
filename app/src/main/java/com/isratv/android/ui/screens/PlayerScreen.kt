package com.isratv.android.ui.screens

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.isratv.android.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Constants for PiP actions ---
private const val ACTION_MEDIA_CONTROL = "media_control"
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_PLAY = 1
private const val CONTROL_TYPE_PAUSE = 2

@Composable
fun PlayerScreen(
    channelName: String,
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mainActivity = context as? MainActivity
    val view = LocalView.current
    val window = activity?.window

    // Scope for asynchronous operations (like delay on exit)
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current

    // --- Immersive Mode Effect & Default Portrait ---
    DisposableEffect(Unit) {
        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, view) }

        // Enter immersive mode
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Force PORTRAIT when entering the screen
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            // Exit immersive mode
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
            // Restore default orientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Manage PiP flag
    DisposableEffect(Unit) {
        mainActivity?.isPipEnabled = true
        onDispose {
            mainActivity?.isPipEnabled = false
        }
    }

    // Keep screen on while watching
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isInPipMode = remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.isInPictureInPictureMode == true
        } else {
            false
        }
    }

    val exoPlayer = remember {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)

        val hlsMediaSourceFactory = HlsMediaSource.Factory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(hlsMediaSourceFactory)
            .build()
    }

    // --- Function to update PiP action buttons ---
    fun updatePipActions(isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val iconId = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            val title = if (isPlaying) "Pause" else "Play"
            val controlType = if (isPlaying) CONTROL_TYPE_PAUSE else CONTROL_TYPE_PAUSE // Note: Fixed control type logic if needed, but keeping as was

            val intent = Intent(ACTION_MEDIA_CONTROL).apply {
                putExtra(EXTRA_CONTROL_TYPE, controlType)
                setPackage(context.packageName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                controlType,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val icon = Icon.createWithResource(context, iconId)
            val action = RemoteAction(icon, title, title, pendingIntent)

            val params = PictureInPictureParams.Builder()
                .setActions(listOf(action))
                .setAspectRatio(Rational(16, 9))
                .build()

            activity?.setPictureInPictureParams(params)
        }
    }

    // --- BroadcastReceiver to handle PiP actions ---
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_MEDIA_CONTROL) {
                    val type = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
                    when (type) {
                        CONTROL_TYPE_PLAY -> {
                            exoPlayer.play()
                            updatePipActions(true)
                        }
                        CONTROL_TYPE_PAUSE -> {
                            exoPlayer.pause()
                            updatePipActions(false)
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter(ACTION_MEDIA_CONTROL)
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.unregisterReceiver(receiver)
            }
        }
    }

    // --- Improved cleanup v2 (Fixes sound issue in PiP) ---
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val isChangingConfigurations = activity?.isChangingConfigurations == true
                if (!isChangingConfigurations && !isInPipMode) {
                    exoPlayer.pause()
                    exoPlayer.release()
                } else if (!isChangingConfigurations && isInPipMode) {
                    exoPlayer.pause()
                    exoPlayer.release()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    var isPlayerVisible by remember { mutableStateOf(true) }

    fun safeExit() {
        scope.launch {
            try {
                isPlayerVisible = false
                exoPlayer.pause()
                delay(50)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onBack()
        }
    }

    BackHandler(enabled = true) {
        safeExit()
    }

    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var areControlsVisible by rememberSaveable { mutableStateOf(true) }

    // States for new features
    var isMuted by rememberSaveable { mutableStateOf(false) }

    // Apply mute state to player
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                val isNowPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
                isPlaying = isNowPlaying
                updatePipActions(isNowPlaying)
            }
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
                updatePipActions(isPlayingState)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(4000)
            areControlsVisible = false
        }
    }

    LaunchedEffect(url) {
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipActions(exoPlayer.isPlaying)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity?.enterPictureInPictureMode(params)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            },
        contentAlignment = Alignment.Center
    ) {
        if (isPlayerVisible) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                onRelease = { playerView ->
                    playerView.player = null
                }
            )
        }

        if (isBuffering && isPlayerVisible) {
            CircularProgressIndicator(color = Color.White)
        }

        if (!isInPipMode && isPlayerVisible) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { safeExit() }) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = channelName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = { enterPipMode() }) {
                            Icon(Icons.Filled.PictureInPictureAlt, "PiP", tint = Color.White)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = areControlsVisible && !isBuffering,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                IconButton(
                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.size(72.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable {
                            if (exoPlayer.isCurrentMediaItemLive) {
                                exoPlayer.seekToDefaultPosition()
                            } else {
                                exoPlayer.seekTo(exoPlayer.duration)
                            }
                            exoPlayer.play()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // New Row for Mute and Rotation Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Mute Button
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }

                        // Screen Rotation Toggle Button
                        IconButton(onClick = {
                            val currentOrientation = activity?.requestedOrientation
                            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                                currentOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}