package com.pingmonitor.data

actual class NotifierImpl actual constructor() : NotifierRepository {
    override fun notify(title: String, message: String, notificationId: Int) {
        // iOS: requiere UNUserNotificationCenter — stub por ahora
    }
}
