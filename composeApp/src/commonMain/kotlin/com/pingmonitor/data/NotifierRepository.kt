package com.pingmonitor.data

interface NotifierRepository {
    fun notify(title: String, message: String, notificationId: Int = 1001)
}
