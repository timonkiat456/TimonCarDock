package com.example.autodeck

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate

class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    private val surfaceCallback = DashboardSurfaceCallback(carContext)

    init {
        // MUST register surface callback with AppManager on screen initialization
        carContext.getCarService(androidx.car.app.AppManager::class.java)
            .setSurfaceCallback(surfaceCallback)
    }

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Refresh")
                    .setOnClickListener {
                        // Action click handler
                    }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .build()
    }
}
