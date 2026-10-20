package com.example.autodeck

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        val settings: WebSettings = webView.settings

        // JavaScript & DOM Storage Setup
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        // Geolocation Settings
        settings.setGeolocationEnabled(true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // When player.html finishes loading, inject current playing metadata
                if (url?.contains("player.html") == true) {
                    syncCurrentMediaToWebView()
                }
            }
        }

        // Check GPS permissions
        checkAndRequestLocationPermission()

        // Initialize Live Media Listener
        setupMediaSessionListener()

        // Load local dashboard
        webView.loadUrl("file:///android_asset/timonauto.html")
    }

    /* -------------------------------------------------------------------------- */
    /*                         LIVE MEDIA SESSION LISTENER                         */
    /* -------------------------------------------------------------------------- */

    private fun setupMediaSessionListener() {
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        try {
            // Check active media controllers (e.g. YT Music, Spotify)
            val controllers = mediaSessionManager.getActiveSessions(null)

            if (controllers.isNotEmpty()) {
                attachMediaController(controllers[0])
            }

            mediaSessionManager.addOnActiveSessionsChangedListener(
                { activeControllers ->
                    if (!activeControllers.isNullOrEmpty()) {
                        attachMediaController(activeControllers[0])
                    }
                },
                null
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun attachMediaController(controller: MediaController) {
        activeController = controller

        // Listen for metadata changes (Song Title, Artist, Cover Artwork)
        controller.registerCallback(object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                super.onMetadataChanged(metadata)
                syncCurrentMediaToWebView()
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                super.onPlaybackStateChanged(state)
                syncCurrentMediaToWebView()
            }
        })

        syncCurrentMediaToWebView()
    }

    /* -------------------------------------------------------------------------- */
    /*                         DYNAMIC JAVASCRIPT INJECTION                      */
    /* -------------------------------------------------------------------------- */

    private fun syncCurrentMediaToWebView() {
        val metadata = activeController?.metadata ?: return

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val artworkUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) ?: ""

        val lrcLyrics = fetchLrcLyricsForTrack(title, artist)

        // Construct dynamic JSON Payload safely using JSONObject
        val payload = JSONObject().apply {
            put("title", title)
            put("artist", artist)
            put("coverUrl", artworkUri)
            put("lrcLyrics", lrcLyrics)
        }

        runOnUiThread {
            webView.evaluateJavascript("if (typeof loadSongData === 'function') { loadSongData($payload); }", null)
        }
    }

    private fun fetchLrcLyricsForTrack(title: String, artist: String): String {
        return "[00:00.00] $title - $artist\n[00:02.00] Syncing live track data..."
    }

    /* -------------------------------------------------------------------------- */
    /*                            PERMISSIONS HANDLING                            */
    /* -------------------------------------------------------------------------- */

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                webView.reload()
            }
        }
    }
}
