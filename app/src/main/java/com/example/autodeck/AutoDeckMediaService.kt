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
        
        // Initialize the media session so Android Auto recognizes a valid token
        mediaSession = MediaSessionCompat(this, "AutoDeckMediaService").apply {
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
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

        if (parentId == "autodeck_root") {
            val description = MediaDescriptionCompat.Builder()
                .setMediaId("autodeck_dashboard_item")
                .setTitle("AutoDeck Dashboard")
                .setSubtitle("Tap to load dashboard")
                .build()

            mediaItems.add(
                MediaBrowserCompat.MediaItem(
                    description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            )
        }

        result.sendResult(mediaItems)
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }
}
