package com.example.autodeck

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.CarContext

class AutoDeckCarService : CarAppService() {
    override fun onCreateSession(): Session {
        return AutoDeckSession()
    }
}
