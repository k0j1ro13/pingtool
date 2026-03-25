package com.pingmonitor.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID   = "pingtool_alerts"
private const val CHANNEL_NAME = "Alertas de red"

actual class NotifierImpl actual constructor() : NotifierRepository {

    companion object {
        var appContext: Context? = null
    }

    override fun notify(title: String, message: String, notificationId: Int) {
        val ctx = appContext ?: return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal (idempotente a partir de API 26)
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }
}
