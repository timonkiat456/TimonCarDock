package com.example.autodeck

import androidx.car.app.Session
import androidx.car.app.Screen
import androidx.car.app.CarContext
import androidx.car.app.model.Template
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.Row
import androidx.car.app.AppManager

class AutoDeckSession : Session() {
    override fun onCreateScreen(carContext: CarContext): Screen {
        return AutoDeckScreen(carContext)
    }
}

class AutoDeckScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        // This hooks up your WebView surface callback to Android Auto
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(DashboardSurfaceCallback(carContext))

        // Android Auto requires a valid template to display the app
        return PaneTemplate.Builder(
            Pane.Builder().addRow(
                Row.Builder().setTitle("AutoDeck Dashboard Ready").build()
            ).build()
        ).setTitle("AutoDeck").build()
    }
}
