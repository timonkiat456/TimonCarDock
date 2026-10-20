package com.example.autodeck

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

class AutoDeckCarService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        return AutoDeckSession()
    }
}

class AutoDeckSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return AutoDeckDashboardScreen(carContext)
    }
}

class AutoDeckDashboardScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val row1 = Row.Builder()
            .setTitle("AutoDeck Browser Running")
            .addText("Active playback session connected to mobile web engine.")
            .build()

        val row2 = Row.Builder()
            .setTitle("YouTube Web Control")
            .addText("Use your connected phone interface for search and video navigation.")
            .build()

        val itemList = ItemList.Builder()
            .addItem(row1)
            .addItem(row2)
            .build()

        return ListTemplate.Builder()
            .setTitle("AutoDeck Dashboard")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemList)
            .build()
    }
}
