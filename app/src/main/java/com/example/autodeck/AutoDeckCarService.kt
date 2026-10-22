package com.example.autodeck

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.CarContext
import androidx.car.app.validation.HostValidator

class AutoDeckCarService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Use ALLOW_ALL_HOSTS_VALIDATOR for development/sideloading debug builds,
        // or strict signature validation for production release.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AutoDeckSession()
    }
}
