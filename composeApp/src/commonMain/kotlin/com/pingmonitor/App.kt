package com.pingmonitor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pingmonitor.ui.MainScreen
import com.pingmonitor.ui.PingToolTheme

@Composable
fun App() {
    var isDarkTheme by remember { mutableStateOf(true) }
    PingToolTheme(darkTheme = isDarkTheme) {
        MainScreen(
            isDarkTheme = isDarkTheme,
            onToggleTheme = { isDarkTheme = !isDarkTheme }
        )
    }
}
