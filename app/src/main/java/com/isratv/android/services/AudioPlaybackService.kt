package com.isratv.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.isratv.android.MainActivity
import com.isratv.android.R

class AudioPlaybackService : Service() {

    // יצירת ערוץ תקשורת ישיר עם הנגן באפליקציה
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
                override fun onPlay() {
                    mediaControlListener?.onPlay()
                }
                override fun onPause() {
                    mediaControlListener?.onPause()
                }
            })
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // כשלוחצים על הכפתור בהתראה, זה פונה ישר לנגן!
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
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.isActive = false
        mediaSession?.release()
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

        // פקודות שמפעילות את הפונקציה onStartCommand שלנו
        val playIntent = Intent(this, AudioPlaybackService::class.java).apply { action = "ACTION_PLAY" }
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, AudioPlaybackService::class.java).apply { action = "ACTION_PAUSE" }
        val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val action = if (isPlaying) {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", playPendingIntent)
        }

        val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, channelName)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "שידור חי")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1L) // המערכת תזהה את זה כ-LIVE ותעלים את סרגל הזמן
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIconBitmap)
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
            .setLargeIcon(largeIconBitmap)
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

        // המשתנה הגלובלי שיחזיק את הקשר לנגן במסך ה-Compose
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