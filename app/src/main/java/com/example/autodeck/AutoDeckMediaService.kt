package com.example.autodeck

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat

class AutoDeckMediaService : MediaBrowserServiceCompat() {

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // Accept connection from Android Auto system
        return BrowserRoot("root_id", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        if (parentId == "root_id") {
            // Provide a clickable item that represents your dashboard/player view
            val description = MediaDescriptionCompat.Builder()
                .setMediaId("dashboard_item")
                .setTitle("AutoDeck Dashboard")
                .setSubtitle("Live Lyrics & Media")
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
}
