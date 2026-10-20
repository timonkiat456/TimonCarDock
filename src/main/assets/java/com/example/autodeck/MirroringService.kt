package com.example.autodeck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationCompat

class MirroringService : Service() {

    companion object {
        private var mediaProjection: MediaProjection? = null
        private var virtualDisplay: VirtualDisplay? = null

        fun setMediaProjection(projection: MediaProjection) {
            mediaProjection = projection
        }

        fun startProjection(context: Context, surface: Surface) {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AutoDeckDisplay",
                1280, 720, 240,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null
            )
        }

        fun stopProjection() {
            virtualDisplay?.release()
            virtualDisplay = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "AUTODECK_CHANNEL")
            .setContentTitle("AutoDeck Active")
            .setContentText("Streaming dashboard to Android Auto...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(101, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AUTODECK_CHANNEL",
                "AutoDeck Stream Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
