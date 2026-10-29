package com.example.autodeck

import android.content.Intent
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.surface.SurfaceCallback // <-- Added correct import for SurfaceCallback

class AutoDeckSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return DashboardMainScreen(carContext)
    }
}

class DashboardMainScreen(carContext: CarContext) : Screen(carContext) {

    private val surfaceCallback = DashboardSurfaceCallback(carContext)

    init {
        // Safely attach the surface callback when the screen initializes
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(surfaceCallback as SurfaceCallback)
    }

    override fun onGetTemplate(): Template {
        // Android Auto REQUIRES an ActionStrip on NavigationTemplate
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Dashboard")
                    .setOnClickListener {
                        // Refresh or menu action handler
                    }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .build()
    }
}
