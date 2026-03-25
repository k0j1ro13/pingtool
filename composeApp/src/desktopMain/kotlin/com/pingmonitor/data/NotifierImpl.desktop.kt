package com.pingmonitor.data

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

actual class NotifierImpl actual constructor() : NotifierRepository {

    private var trayIcon: TrayIcon? = null

    override fun notify(title: String, message: String, notificationId: Int) {
        if (!SystemTray.isSupported()) return
        try {
            val tray = SystemTray.getSystemTray()
            if (trayIcon == null) {
                val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
                trayIcon = TrayIcon(image, "PingTool").also {
                    it.isImageAutoSize = true
                    tray.add(it)
                }
            }
            trayIcon?.displayMessage(title, message, TrayIcon.MessageType.WARNING)
        } catch (_: Exception) {}
    }
}
