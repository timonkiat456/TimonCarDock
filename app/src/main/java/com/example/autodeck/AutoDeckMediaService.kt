package com.example.autodeck

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat

class AutoDeckMediaService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "AutoDeckMediaService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Register callback to capture play commands from Android Auto
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    handleLaunch()
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    handleLaunch()
                }
            })

            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .build()

            setPlaybackState(state)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
    }

    private fun handleLaunch() {
        // Bring MainActivity to the foreground on the phone / virtual display
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("autodeck_root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        val description = MediaDescriptionCompat.Builder()
            .setMediaId("autodeck_dashboard")
            .setTitle("AutoDeck Dashboard")
            .setSubtitle("Tap to launch")
            .build()

        mediaItems.add(
            MediaBrowserCompat.MediaItem(
                description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        )

        result.sendResult(mediaItems)
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }
}
