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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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

        if (surface == null || width <= 0 || height <= 0) return

        Handler(Looper.getMainLooper()).post {
            try {
                virtualDisplay = carContext.getSystemService(DisplayManager::class.java)?.createVirtualDisplay(
                    "AutoDeckVirtualDisplay",
                    width,
                    height,
                    surfaceContainer.dpi,
                    surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
                )

                val display = virtualDisplay?.display ?: return@post
                presentation = Presentation(carContext, display).apply {
                    window?.setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
                }

                carWebView = WebView(carContext).apply {
                    isClickable = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus(View.FOCUS_DOWN)

                    // Force GPU hardware acceleration for smooth UI performance
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // Cookie Sync Bridge
                    val systemCookieManager = android.webkit.CookieManager.getInstance()
                    systemCookieManager.setAcceptCookie(true)
                    
                    val sessionCookies = systemCookieManager.getCookie("https://youtube.com") 
                        ?: systemCookieManager.getCookie("https://google.com")
                    
                    if (!sessionCookies.isNullOrEmpty()) {
                        val carCookieManager = android.webkit.CookieManager.getInstance()
                        carCookieManager.setAcceptCookie(true)
                        carCookieManager.setAcceptThirdPartyCookies(this, true)
                        carCookieManager.setCookie("https://www.youtube.com", sessionCookies)
                        carCookieManager.setCookie("https://music.youtube.com", sessionCookies)
                        carCookieManager.flush()
                        Log.d("AutoDeckDebug", "Successfully synced session cookies to car WebView!")
                    } else {
                        Log.d("AutoDeckDebug", "No active session found. Please log in via phone browser first.")
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        mixedContentMode = WebSettings
                        
