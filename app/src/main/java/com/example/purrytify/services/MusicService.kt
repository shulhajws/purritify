package com.example.purrytify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.model.Song
import com.example.purrytify.ui.playback.PlayerViewModel

class MusicService : Service() {

    private val binder = MusicBinder()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private var currentSong: Song? = null

    private var playerViewModel: PlayerViewModel? = null

    companion object {
        const val CHANNEL_ID = "MusicServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    fun setPlayerViewModel(viewModel: PlayerViewModel) {
        Log.d("MusicService", "Setting PlayerViewModel: $viewModel")

        playerViewModel = viewModel
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "Music Service created")
        mediaSession = MediaSessionCompat(this, "MusicService")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try {
            createNotificationChannel()
            Log.d("MusicService", "Notification channel created successfully")
        } catch (e: Exception) {
            Log.e("MusicService", "Error creating notification channel: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("MusicService", "onBind called with intent: $intent")
        return binder
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun startForegroundWithNotification(song: Song, isPlaying: Boolean) {
        Log.d("MusicService", "Starting foreground with notification for song: ${song.title}, isPlaying: $isPlaying")

        currentSong = song
        try {
            val notification = createNotification(song, isPlaying)
            startForeground(NOTIFICATION_ID, notification)
            Log.d("MusicService", "Foreground started successfully with notification for song: ${song.title}, isPlaying: $isPlaying")
        } catch (e: Exception) {
            Log.e("MusicService", "Error starting foreground with notification: ${e.message}", e)
        }
    }

    fun updateNotification(isPlaying: Boolean) {
        currentSong?.let {
            Log.d("MusicService", "Updating notification for song: ${it.title}, isPlaying: $isPlaying")

            try {
                val notification = createNotification(it, isPlaying)
                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d("MusicService", "Notification updated successfully")
            } catch (e: Exception) {
                Log.e("MusicService", "Error updating notification: ${e.message}", e)
            }
        } ?: Log.w("MusicService", "No current song to update notification")
    }

    private fun createNotification(song: Song, isPlaying: Boolean): Notification {
        Log.d("MusicService", "Creating notification for song: ${song.title}, isPlaying: $isPlaying")

        try {
            val playPauseAction = if (isPlaying) {
                NotificationCompat.Action(
                    R.drawable.ic_pause_filled, "Pause",
                    getActionIntent("ACTION_PAUSE")
                )
            } else {
                NotificationCompat.Action(
                    R.drawable.ic_play_filled, "Play",
                    getActionIntent("ACTION_PLAY")
                )
            }

            val nextAction = NotificationCompat.Action(
                R.drawable.ic_next, "Next",
                getActionIntent("ACTION_NEXT")
            )

            val prevAction = NotificationCompat.Action(
                R.drawable.ic_prev, "Previous",
                getActionIntent("ACTION_PREVIOUS")
            )

            val contentIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).apply {
                    putExtra("songId", song.id)
                    putExtra("song", song)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Convert to Bitmap
            val albumArtBitmap = BitmapFactory.decodeFile(song.albumArt)

            // The returns:
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(albumArtBitmap)
                .setContentIntent(contentIntent)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build().also {
                    Log.d("MusicService", "Notification created successfully for song: ${song.title}, isPlaying from song: ${song.isPlaying}, isPlaying from playerViewModel: $isPlaying")
                }
        } catch (e: Exception) {
            Log.e("MusicService", "Error creating notification: ${e.message}", e)
            throw e
        }
    }

    private fun getActionIntent(action: String): PendingIntent {
        Log.d("MusicService", "Creating action intent for action: $action")

        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        // Just for safety
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d("MusicService", "Creating notification channel with, android.os.Build.VERSION.SDK_INT = ${android.os.Build.VERSION.SDK_INT}")

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            try {
                notificationManager.createNotificationChannel(channel)
                Log.d("MusicService", "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e("MusicService", "Error creating notification channel: ${e.message}", e)
            }
        }
    }

    // Handle actions on notification control
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("MusicService", "Check for playerViewModel: $playerViewModel")
        when (intent?.action) {
            "ACTION_PLAY" -> {
                // Play song
                playerViewModel?.togglePlayPause()
                Log.d("MusicService", "Toggled play")
            }
            "ACTION_PAUSE" -> {
                // Pause song
                playerViewModel?.togglePlayPause()
                Log.d("MusicService", "Toggled pause")
            }
            "ACTION_NEXT" -> {
                // Play next song
                playerViewModel?.playNext()
                Log.d("MusicService", "Playing next song")
            }
            "ACTION_PREVIOUS" -> {
                // Play previous song
                playerViewModel?.playPrevious()
                Log.d("MusicService", "Playing previous song")
            }
            else -> {
                Log.w("MusicService", "Unknown action: ${intent?.action}, with intent: $intent")
            }
        }
        return START_NOT_STICKY
    }

}