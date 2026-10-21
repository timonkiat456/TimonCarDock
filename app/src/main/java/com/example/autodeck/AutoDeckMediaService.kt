package com.example.autodeck

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

        // Create media session and set active playback state
        mediaSession = MediaSessionCompat(this, "AutoDeckMediaService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            
            // Set state to NONE/STOPPED so AA knows the session is ready
            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
            setPlaybackState(state)
            
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // Return root ID unconditionally
        return BrowserRoot("root_id", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        // Create a root item that Android Auto can render as a playable button
        val description = MediaDescriptionCompat.Builder()
            .setMediaId("autodeck_dashboard")
            .setTitle("Open Dashboard")
            .setSubtitle("Tap to launch AutoDeck WebView")
            .build()

        mediaItems.add(
            MediaBrowserCompat.MediaItem(
                description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        )

        // Must send result synchronously
        result.sendResult(mediaItems)
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }
}
