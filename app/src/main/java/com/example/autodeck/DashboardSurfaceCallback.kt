package com.example.autodeck

import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer

class DashboardSurfaceCallback(private val carContext: CarContext) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        if (surfaceContainer.width == 0 || surfaceContainer.height == 0) return

        // 1. Create a VirtualDisplay bound to the car screen's surface buffer
        virtualDisplay = carContext.getSystemService(DisplayManager::class.java).createVirtualDisplay(
            "AutoDeckVirtualDisplay",
            surfaceContainer.width,
            surfaceContainer.height,
            surfaceContainer.dpi,
            surfaceContainer.surface,
            0
        )

        val display = virtualDisplay?.display ?: return

        // 2. Instantiate a Presentation UI on that display
        presentation = Presentation(carContext, display)

        // 3. Create a WebView and load your existing HTML/JS assets
        val webView = WebView(carContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadUrl("file:///android_asset/index.html") // Points to your HTML asset
        }

        presentation?.setContentView(webView)
        presentation?.show()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        presentation?.dismiss()
        virtualDisplay?.release()
    }
}
