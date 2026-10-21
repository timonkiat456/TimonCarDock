package com.example.autodeck

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class AutoDeckSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return DashboardMainScreen(carContext)
    }
}

class DashboardMainScreen(carContext: CarContext) : Screen(carContext) {

    private var isSystemActive = true

    override fun onGetTemplate(): Template {
        val statusRow = Row.Builder()
            .setTitle("AutoDeck Status")
            .addText(if (isSystemActive) "Status: Online | Native Template Running" else "Status: Paused")
            .build()

        val metricsRow = Row.Builder()
            .setTitle("Vehicle Telemetry")
            .addText("Haval H6 HEV Dashboard Link Active")
            .build()

        val pane = Pane.Builder()
            .addRow(statusRow)
            .addRow(metricsRow)
            .addAction(
                Action.Builder()
                    .setTitle(if (isSystemActive) "Pause Dashboard" else "Resume Dashboard")
                    .setOnClickListener {
                        isSystemActive = !isSystemActive
                        invalidate() // Triggers UI redraw on the car screen
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(
                Header.Builder()
                    .setTitle("AutoDeck Control Center")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }
}
