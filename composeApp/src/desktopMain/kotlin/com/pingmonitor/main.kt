package com.pingmonitor

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pingmonitor.di.appModule
import java.awt.Dimension
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }

    application {
        val windowState = rememberWindowState(size = DpSize(720.dp, 700.dp))

        Window(
            onCloseRequest = ::exitApplication,
            title = "PingTool",
            state = windowState
        ) {
            // Tamaño mínimo para que la interfaz no se comprima
            window.minimumSize = Dimension(580, 520)
            App()
        }
    }
}
