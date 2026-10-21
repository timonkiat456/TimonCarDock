package com.example.autodeck

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
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

    // Mock state variable for dynamic UI testing
    private var isSystemActive = true

    override fun onGetTemplate(): Template {
        // 1. Build status summary row
        val statusRow = Row.Builder()
            .setTitle("AutoDeck Status")
            .addText(if (isSystemActive) "Status: Online | All Services Running" else "Status: Offline")
            .build()

        // 2. Build detailed metrics row
        val metricsRow = Row.Builder()
            .setTitle("Vehicle Telemetry")
            .addText("Connection: Active | High-Speed Sync")
            .build()

        // 3. Assemble rows into a Pane
        val pane = Pane.Builder()
            .addRow(statusRow)
            .addRow(metricsRow)
            .addAction(
                Action.Builder()
                    .setTitle(if (isSystemActive) "Pause Dashboard" else "Start Dashboard")
                    .setOnClickListener {
                        isSystemActive = !isSystemActive
                        invalidate() // Request UI re-render
                    }
                    .build()
            )
            .build()

        // 4. Return native PaneTemplate with Header
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
