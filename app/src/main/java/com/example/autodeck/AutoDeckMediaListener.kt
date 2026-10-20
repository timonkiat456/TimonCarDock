package com.example.autodeck

import android.service.notification.NotificationListenerService

class AutoDeckMediaListener : NotificationListenerService() {
    // This empty service forces Android to recognize AutoDeck as a media/notification reader,
    // causing it to finally appear in the system's "Notification Access" settings list.
}
