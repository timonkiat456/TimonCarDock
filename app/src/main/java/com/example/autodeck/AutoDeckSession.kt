package com.example.autodeck

import android.content.Intent
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate

class AutoDeckSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        // Register the surface callback with Android Auto to intercept the drawing surface
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(DashboardSurfaceCallback(carContext))

        return DashboardMainScreen(carContext)
    }
}

class DashboardMainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        // NavigationTemplate instructs Android Auto to allocate a full surface canvas
        return NavigationTemplate.Builder().build()
    }
}
