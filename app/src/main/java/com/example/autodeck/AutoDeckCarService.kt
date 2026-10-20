package com.example.autodeck

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class AutoDeckCarService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // Allows sideloaded/developer hosts to run this service
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return AutoDeckCarScreen(carContext)
            }
        }
    }
}

class AutoDeckCarScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("AutoDeck Dashboard Ready")
            .setTitle("AutoDeck")
            .setHeaderAction(Action.APP_ICON)
            .addAction(
                Action.Builder()
                    .setTitle("Open Dashboard")
                    .setOnClickListener {
                        val intent = carContext.packageManager.getLaunchIntentForPackage(carContext.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        carContext.startActivity(intent)
                    }
                    .build()
            )
            .build()
    }
}
