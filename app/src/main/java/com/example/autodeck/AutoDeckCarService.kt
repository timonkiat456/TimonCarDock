package com.example.autodeck

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class AutoDeckCarService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allows connection from sideloaded or debug head units
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AutoDeckSession()
    }
}
