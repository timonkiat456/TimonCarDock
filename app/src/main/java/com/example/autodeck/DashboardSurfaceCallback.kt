package com.example.autodeck

import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer

class DashboardSurfaceCallback(private val carContext: CarContext) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null

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
                val webView = WebView(carContext).apply {
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
                    loadUrl("file:///android_asset/player.html")
                }

                presentation?.setContentView(webView)
                presentation?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Handler(Looper.getMainLooper()).post {
            try {
                presentation?.dismiss()
                presentation = null
                virtualDisplay?.release()
                virtualDisplay = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
