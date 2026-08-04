package com.isratv.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.isratv.android.MainActivity
import com.isratv.android.R

class AudioPlaybackService : Service() {

    interface MediaControlListener {
        fun onPlay()
        fun onPause()
    }

    private var mediaSession: MediaSessionCompat? = null
    private var currentChannelName: String = "TvStreams"
    private var isPlaying: Boolean = true

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "AudioPlaybackService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { mediaControlListener?.onPlay() }
                override fun onPause() { mediaControlListener?.onPause() }
            })
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                "ACTION_PLAY" -> mediaControlListener?.onPlay()
                "ACTION_PAUSE" -> mediaControlListener?.onPause()
            }
        }

        intent?.let {
            if (it.hasExtra("CHANNEL_NAME")) {
                currentChannelName = it.getStringExtra("CHANNEL_NAME") ?: "TvStreams"
            }
            if (it.hasExtra("IS_PLAYING")) {
                isPlaying = it.getBooleanExtra("IS_PLAYING", true)
            }
        }

        val notification = createMediaNotification(currentChannelName, isPlaying)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopForeground(true) // Added to forcibly remove the notification
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true) // Added to forcibly remove the notification
        mediaSession?.isActive = false
        mediaSession?.release()
    }

    // מיפוי מדויק ותופס כל וריאציה של שמות הערוצים
    private fun getChannelLogoBitmap(channelName: String): Bitmap {
        val drawableResId = when {
            channelName.contains("kan", ignoreCase = true) || channelName.contains("11", ignoreCase = true) -> R.drawable.kan_11_il
            channelName.contains("keshet", ignoreCase = true) || channelName.contains("12", ignoreCase = true) -> R.drawable.keshet_12_il
            channelName.contains("reshet", ignoreCase = true) || channelName.contains("13", ignoreCase = true) -> R.drawable.reshet_13_il
            channelName.contains("now", ignoreCase = true) || channelName.contains("14", ignoreCase = true) -> R.drawable.now_14_il
            channelName.contains("i24", ignoreCase = true) -> R.drawable.i24_news_il
            channelName.contains("knesset", ignoreCase = true) -> R.drawable.knesset_channel_il
            else -> R.mipmap.ic_launcher
        }

        val rawBitmap = BitmapFactory.decodeResource(resources, drawableResId)
        return beautifyBitmapForMediaStyle(rawBitmap)
    }

    // פונקציה שמתאימה את יחס התמונה (Beautify) כך שלא תיראה מרוחה במסך ההתראות
    private fun beautifyBitmapForMediaStyle(source: Bitmap): Bitmap {
        val targetWidth = 512
        val targetHeight = 512
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // צביעת רקע כהה ואלגנטי למילוי השוליים (מונע מתיחה מכוערת)
        canvas.drawColor(Color.parseColor("#121212"))

        val srcWidth = source.width.toFloat()
        val srcHeight = source.height.toFloat()

        val scale = minOf(targetWidth / srcWidth, targetHeight / srcHeight)
        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale

        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val targetRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(source, null, targetRect, Paint(Paint.FILTER_BITMAP_FLAG))

        return output
    }

    private fun createMediaNotification(channelName: String, isPlaying: Boolean): Notification {
        val channelId = "audio_playback_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setSound(null, null) }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenAppIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = Intent(this, AudioPlaybackService::class.java).apply { action = "ACTION_PLAY" }
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, AudioPlaybackService::class.java).apply { action = "ACTION_PAUSE" }
        val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val action = if (isPlaying) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", playPendingIntent)
        }

        val channelLogo = getChannelLogoBitmap(channelName)

        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, channelName)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "שידור חי")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1L)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, channelLogo)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, channelLogo)
                .build()
        )

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(channelName)
            .setContentText("מנגן ברקע")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(channelLogo)
            .setContentIntent(pendingOpenAppIntent)
            .addAction(action)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setOngoing(isPlaying)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        var mediaControlListener: MediaControlListener? = null

        fun startService(context: Context, channelName: String, isPlaying: Boolean = true) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                putExtra("CHANNEL_NAME", channelName)
                putExtra("IS_PLAYING", isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updatePlaybackState(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                putExtra("IS_PLAYING", isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java)
            context.stopService(intent)
        }
    }
}