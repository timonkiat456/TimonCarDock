package com.example.autodeck

import android.app.Presentation
import android.content.ComponentName
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DashboardSurfaceCallback(private val carContext: CarContext) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var carWebView: WebView? = null

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    private var lastArtworkUri: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val positionUpdater = object : Runnable {
        override fun run() {
            activeController?.playbackState?.let { state ->
                if (state.state == PlaybackState.STATE_PLAYING) {
                    val currentPositionSec = state.position / 1000.0
                    carWebView?.post {
                        val script = "if (typeof updatePlaybackTime === 'function') { updatePlaybackTime($currentPositionSec); }"
                        carWebView?.evaluateJavascript(script, null)
                    }
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface
        val width = surfaceContainer.width
        val height = surfaceContainer.height

        // Guard against uninitialized dimensions (0x0 surface causes crash)
        if (surface == null || width <= 0 || height <= 0) return

        // Force UI thread execution for WebView and Presentation creation
        Handler(Looper.getMainLooper()).post {
            try {
                // 1. Create the VirtualDisplay on the car's surface buffer
                virtualDisplay = carContext.getSystemService(DisplayManager::class.java)?.createVirtualDisplay(
                    "AutoDeckVirtualDisplay",
                    width,
                    height,
                    surfaceContainer.dpi,
                    surface,
                    0
                )

                val display = virtualDisplay?.display ?: return@post

                // 2. Instantiate Presentation container
                presentation = Presentation(carContext, display)

                // 3. Instantiate WebView safely on the main thread
                carWebView = WebView(carContext).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                    }
                    loadUrl("file:///android_asset/timonauto.html")
                }

                presentation?.setContentView(carWebView!!)
                presentation?.show()

                // 4. Initialize media session monitoring for the car display
                setupMediaSessionListener()
                handler.post(positionUpdater)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupMediaSessionListener() {
        mediaSessionManager = carContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            val listenerComponent = ComponentName(carContext, AutoDeckMediaListener::class.java)
            val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
            Log.d("AutoDeckDebug", "Car active controllers found: ${controllers.size}")

            if (controllers.isNotEmpty()) {
                attachMediaController(controllers[0])
            }

            mediaSessionManager.addOnActiveSessionsChangedListener(
                { activeControllers ->
                    Log.d("AutoDeckDebug", "Car active controllers changed: ${activeControllers?.size}")
                    if (!activeControllers.isNullOrEmpty()) {
                        attachMediaController(activeControllers[0])
                    }
                },
                listenerComponent
            )
        } catch (e: SecurityException) {
            Log.e("AutoDeckDebug", "SecurityException: Notification Access not granted on car session", e)
        }
    }

    private fun attachMediaController(controller: MediaController) {
        activeController = controller
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

    private fun syncCurrentMediaToWebView() {
        val metadata = activeController?.metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"

        lastArtworkUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI) ?: ""

        Log.d("AutoDeckDebug", "Car fetching lyrics for: $title by $artist")
        fetchLyricsFromApi(title, artist)
    }

    private fun cleanMetadataString(raw: String): String {
        return raw.replace(Regex("(?i)\\(official video\\)|\\[official video\\]|\\(official audio\\)|\\(lyric video\\)|\\(audio\\)|\\(remix\\)"), "")
            .trim()
    }

    private fun fetchLyricsFromApi(rawTitle: String, rawArtist: String) {
        val title = cleanMetadataString(rawTitle)
        val artist = cleanMetadataString(rawArtist)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val encodedArtist = URLEncoder.encode(artist, "UTF-8")
                val urlString = "https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist"

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "AutoDeckApp (Personal Project)")

                val lyricsText = if (connection.responseCode == 200) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    json.optString("syncedLyrics", "").takeIf { it.isNotEmpty() && it != "null" }
                        ?: json.optString("plainLyrics", "[00:00.00] No synchronized lyrics found.")
                } else {
                    "[00:00.00] Lyrics not found for $rawTitle"
                }

                val payload = JSONObject().apply {
                    put("title", rawTitle)
                    put("artist", rawArtist)
                    put("coverUrl", lastArtworkUri)
                    put("lrcLyrics", lyricsText)
                }

                Handler(Looper.getMainLooper()).post {
                    carWebView?.evaluateJavascript("if (typeof loadSongData === 'function') { loadSongData($payload); }", null)
                }
            } catch (e: Exception) {
                Log.e("AutoDeckDebug", "Error fetching lyrics for car webview", e)
            }
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        handler.removeCallbacks(positionUpdater)
        Handler(Looper.getMainLooper()).post {
            try {
                presentation?.dismiss()
                presentation = null
                virtualDisplay?.release()
                virtualDisplay = null
                carWebView = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
