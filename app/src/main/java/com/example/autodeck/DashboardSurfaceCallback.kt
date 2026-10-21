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
    val surface = surfaceContainer.surface
    val width = surfaceContainer.width
    val height = surfaceContainer.height

    // Guard against uninitialized surface dimensions
    if (surface == null || width <= 0 || height <= 0) return

    try {
        virtualDisplay = carContext.getSystemService(DisplayManager::class.java)?.createVirtualDisplay(
            "AutoDeckVirtualDisplay",
            width,
            height,
            surfaceContainer.dpi,
            surface,
            0
        )

        val display = virtualDisplay?.display ?: return
        presentation = Presentation(carContext, display)

        val webView = WebView(carContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl("file:///android_asset/index.html")
        }

        presentation?.setContentView(webView)
        presentation?.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        presentation?.dismiss()
        virtualDisplay?.release()
    }
}
