package com.example.autodeck


import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    lateinit var mainWebView: WebView
    lateinit var webFrameWebView: WebView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    private var lastArtworkUri: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val positionUpdater = object : Runnable {
        override fun run() {
            activeController?.playbackState?.let { state ->
                if (state.state == PlaybackState.STATE_PLAYING) {
                    val currentPositionSec = state.position / 1000.0
                    val script = "if (typeof updatePlaybackTime === 'function') { updatePlaybackTime($currentPositionSec); }"
                    if (::mainWebView.isInitialized && mainWebView.url?.contains("player.html") == true) {
                        mainWebView.evaluateJavascript(script, null)
                    }
                    if (::webFrameWebView.isInitialized && webFrameWebView.url?.contains("player.html") == true) {
                        webFrameWebView.evaluateJavascript(script, null)
                    }
                }
            }
            handler.postDelayed(this, 500)
        }
    }





    // Replace your old external intent function with this:
    fun openGoogleLoginInApp() {
        showWebFrame("https://accounts.google.com/ServiceLogin?continue=https://www.youtube.com")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main)

        mainWebView = findViewById(R.id.webView)
        webFrameWebView = findViewById(R.id.webFrameWebView)

        configureWebViewSettings(mainWebView)
        configureWebViewSettings(webFrameWebView)

        mainWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("player.html") == true) {
                    syncCurrentMediaToWebView()
                }
            }
        }

        webFrameWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("player.html") == true) {
                    syncCurrentMediaToWebView()
                }
            }
        }

        val geoChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }
        mainWebView.webChromeClient = geoChromeClient
        webFrameWebView.webChromeClient = geoChromeClient

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webFrameWebView.visibility == View.VISIBLE) {
                    if (webFrameWebView.canGoBack()) {
                        webFrameWebView.goBack()
                    } else {
                        showDashboard()
                    }
                } else if (mainWebView.canGoBack()) {
                    mainWebView.goBack()
                } else {
                    mainWebView.loadUrl("file:///android_asset/timonauto.html")
                }
            }
        })

        checkAndRequestLocationPermission()
        setupMediaSessionListener()

        mainWebView.loadUrl("file:///android_asset/timonauto.html")
    }

    private fun configureWebViewSettings(wv: WebView) {
        val settings: WebSettings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.setGeolocationEnabled(true)
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = true
        settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
        
        wv.isClickable = true
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true
    }

    fun showWebFrame(url: String) {
        runOnUiThread {
            if (webFrameWebView.url != url) {
                webFrameWebView.loadUrl(url)
            }
            webFrameWebView.visibility = View.VISIBLE
            mainWebView.visibility = View.GONE
        }
    }

    fun showDashboard() {
        runOnUiThread {
            webFrameWebView.visibility = View.GONE
            mainWebView.visibility = View.VISIBLE
        }
    }

    private fun setupMediaSessionListener() {
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            val listenerComponent = ComponentName(this, AutoDeckMediaListener::class.java)
            val controllers = mediaSessionManager.getActiveSessions(listenerComponent)
            if (controllers.isNotEmpty()) {
                attachMediaController(controllers[0])
            }
            mediaSessionManager.addOnActiveSessionsChangedListener(
                { activeControllers ->
                    if (!activeControllers.isNullOrEmpty()) {
                        attachMediaController(activeControllers[0])
                    }
                },
                listenerComponent
            )
        } catch (e: SecurityException) {
            Log.e("AutoDeckDebug", "SecurityException: Notification Access not granted", e)
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

                runOnUiThread {
                    val script = "if (typeof loadSongData === 'function') { loadSongData($payload); }"
                    if (::mainWebView.isInitialized && mainWebView.url?.contains("player.html") == true) {
                        mainWebView.evaluateJavascript(script, null)
                    }
                    if (::webFrameWebView.isInitialized && webFrameWebView.url?.contains("player.html") == true) {
                        webFrameWebView.evaluateJavascript(script, null)
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoDeckDebug", "Error fetching lyrics", e)
            }
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setupMediaSessionListener()
        handler.post(positionUpdater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(positionUpdater)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mainWebView.reload()
            }
        }
    }
}
