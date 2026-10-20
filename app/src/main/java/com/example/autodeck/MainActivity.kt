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
import android.util.Log
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
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var mainWebView: WebView
    private lateinit var webFrameWebView: WebView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    private var lastArtworkUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainWebView = findViewById(R.id.webView)
        webFrameWebView = findViewById(R.id.webFrameWebView)

        configureWebViewSettings(mainWebView)
        configureWebViewSettings(webFrameWebView)

        // Main Dashboard Client
        mainWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("player.html") == true) {
                    syncCurrentMediaToWebView()
                }
            }
        }

        // External Web Browsing Client (YouTube Music)
        webFrameWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
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

        // Hardware Back Button Handler
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
        settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
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
            Log.d("AutoDeckDebug", "Active controllers found: ${controllers.size}")

            if (controllers.isNotEmpty()) {
                attachMediaController(controllers[0])
            }

            mediaSessionManager.addOnActiveSessionsChangedListener(
                { activeControllers ->
                    Log.d("AutoDeckDebug", "Active controllers changed: ${activeControllers?.size}")
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
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) ?: ""

        Log.d("AutoDeckDebug", "Pushing to WebView -> Title: $title | Artist: $artist")

        val payload = JSONObject().apply {
            put("title", title)
            put("artist", artist)
            put("coverUrl", lastArtworkUri)
            put("lrcLyrics", "[00:00.00] Debug Mode: Title & Artist received successfully!")
        }

        runOnUiThread {
            if (::mainWebView.isInitialized && mainWebView.url?.contains("player.html") == true) {
                mainWebView.evaluateJavascript("if (typeof loadSongData === 'function') { loadSongData($payload); }", null)
            }
        }
    }

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

    override fun onResume() {
        super.onResume()
        setupMediaSessionListener()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mainWebView.reload()
            }
        }
    }
}
