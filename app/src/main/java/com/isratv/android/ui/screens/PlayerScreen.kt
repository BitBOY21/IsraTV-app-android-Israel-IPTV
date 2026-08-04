package com.isratv.android.ui.screens

import com.isratv.android.R
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
import com.isratv.android.services.AudioPlaybackService
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ACTION_MEDIA_CONTROL = "com.isratv.android.MEDIA_CONTROL"
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_PLAY = 1
private const val CONTROL_TYPE_PAUSE = 2
private const val CONTROL_TYPE_HEADPHONES = 3

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

    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var isAudioOnly by rememberSaveable { mutableStateOf(false) }

    // הפעלת השירות עם העברת שם הערוץ כדי שהלוגו הנכון יטען
    DisposableEffect(Unit) {
        AudioPlaybackService.startService(context, channelName, true)

        onDispose {
            AudioPlaybackService.stopService(context)
        }
    }

    DisposableEffect(Unit) {
        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, view) }

        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        mainActivity?.isPipEnabled = true
        onDispose {
            mainActivity?.isPipEnabled = false
        }
    }

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

    DisposableEffect(exoPlayer) {
        AudioPlaybackService.mediaControlListener = object : AudioPlaybackService.MediaControlListener {
            override fun onPlay() {
                exoPlayer.play()
            }
            override fun onPause() {
                exoPlayer.pause()
            }
        }

        onDispose {
            AudioPlaybackService.mediaControlListener = null
        }
    }

    fun toggleAudioOnlyMode(enableAudioOnly: Boolean) {
        isAudioOnly = enableAudioOnly
        val parameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, enableAudioOnly)
            .build()
        exoPlayer.trackSelectionParameters = parameters

        if (enableAudioOnly) {
            activity?.moveTaskToBack(true)
        }
    }

    fun updatePipActions(isPlaying: Boolean, audioOnly: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = mutableListOf<RemoteAction>()

            // 1. כפתור Play / Pause
            val playPauseIconId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            val playPauseTitle = if (isPlaying) "Pause" else "Play"
            val playPauseControlType = if (isPlaying) CONTROL_TYPE_PAUSE else CONTROL_TYPE_PLAY

            val playPauseIntent = Intent(ACTION_MEDIA_CONTROL).apply {
                putExtra(EXTRA_CONTROL_TYPE, playPauseControlType)
                setPackage(context.packageName)
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                playPauseControlType,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPauseAction = RemoteAction(
                Icon.createWithResource(context, playPauseIconId),
                playPauseTitle,
                playPauseTitle,
                playPausePendingIntent
            )

            // 2. כפתור האזנה (Headphones)
            val headphonesIntent = Intent(ACTION_MEDIA_CONTROL).apply {
                putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_HEADPHONES)
                setPackage(context.packageName)
            }
            val headphonesPendingIntent = PendingIntent.getBroadcast(
                context,
                CONTROL_TYPE_HEADPHONES,
                headphonesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val headphonesAction = RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_headphones),
                "Audio Only",
                "Audio Only",
                headphonesPendingIntent
            )

            // 3. כפתור רווח שקוף (Placeholder) שנועד לאזן את המרחב ולדחוף את הכפתורים למרכז/שמאל בצורה מושלמת
            val emptyIntent = Intent("com.isratv.android.EMPTY_ACTION").apply {
                setPackage(context.packageName)
            }
            val emptyPendingIntent = PendingIntent.getBroadcast(
                context,
                999,
                emptyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // שימוש באייקון שקוף או מינימלי שלא יפריע בעין, או בפעולה ריקה
            val spacerAction = RemoteAction(
                Icon.createWithResource(context, android.R.color.transparent),
                "",
                "",
                emptyPendingIntent
            )

            // סידור 3 הרכיבים כך שנוצר רווח מאזן:
            // סדר: [אוזניות] -> [פאוז/פליי] -> [מרווח שקוף]
            // זה מאלץ את אנדרואיד לפרוס אותם על פני הרוחב ולמקם את הפאוז במרכז יחסי מצוין.
            val isRtl = context.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL

            if (isRtl) {
                actions.add(spacerAction)
                actions.add(playPauseAction)
                actions.add(headphonesAction)
            } else {
                actions.add(headphonesAction)
                actions.add(playPauseAction)
                actions.add(spacerAction)
            }

            val params = PictureInPictureParams.Builder()
                .setActions(actions)
                .setAspectRatio(Rational(16, 9))
                .build()

            activity?.setPictureInPictureParams(params)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_MEDIA_CONTROL) {
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_TYPE_PLAY -> {
                            exoPlayer.play()
                        }
                        CONTROL_TYPE_PAUSE -> {
                            exoPlayer.pause()
                        }
                        CONTROL_TYPE_HEADPHONES -> {
                            toggleAudioOnlyMode(!isAudioOnly)
                            updatePipActions(exoPlayer.isPlaying, !isAudioOnly)
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

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (isAudioOnly) {
                        toggleAudioOnlyMode(false)
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (activity?.isFinishing == true) {
                        exoPlayer.pause()
                        AudioPlaybackService.stopService(context)
                    }
                }
                else -> {}
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
                AudioPlaybackService.stopService(context)
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
    var isMuted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                val isNowPlaying = playbackState == Player.STATE_READY && exoPlayer.playWhenReady
                isPlaying = isNowPlaying
                updatePipActions(isNowPlaying, isAudioOnly)
                AudioPlaybackService.updatePlaybackState(context, isNowPlaying)
            }
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
                updatePipActions(isPlayingState, isAudioOnly)
                AudioPlaybackService.updatePlaybackState(context, isPlayingState)
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
            updatePipActions(exoPlayer.isPlaying, isAudioOnly)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { toggleAudioOnlyMode(!isAudioOnly) }) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = "Audio Only",
                                tint = if (isAudioOnly) Color.Green else Color.White
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = { enterPipMode() }) {
                                Icon(Icons.Filled.PictureInPictureAlt, "PiP", tint = Color.White)
                            }
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
                        contentDescription = "Play/Progress",
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }

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