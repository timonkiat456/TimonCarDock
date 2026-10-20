package com.example.autodeck

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

class AutoDeckMediaService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize MediaSessionCompat and assign session token
        mediaSession = MediaSessionCompat(this, "AutoDeckMediaService")
        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // Return a valid root ID
        return BrowserRoot("autodeck_root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        // 2. Return a dummy playable item so Android Auto stops loading
        val description = MediaDescriptionCompat.Builder()
            .setMediaId("autodeck_dashboard_item")
            .setTitle("Open Dashboard")
            .setSubtitle("Tap to launch AutoDeck")
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
