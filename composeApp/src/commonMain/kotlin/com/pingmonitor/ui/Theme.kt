package com.pingmonitor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta PingTool — estética de herramienta de red profesional
private val ColorPrimary         = Color(0xFF00BFA5)  // teal brillante
private val ColorOnPrimary       = Color(0xFF003731)
private val ColorPrimaryContainer = Color(0xFF004D45)
private val ColorOnPrimaryContainer = Color(0xFF70F7E6)

private val ColorSecondary       = Color(0xFF0097A7)
private val ColorOnSecondary     = Color(0xFF001F24)

private val ColorBackground      = Color(0xFF0D1117)  // negro-azulado (GitHub dark)
private val ColorOnBackground    = Color(0xFFE6EDF3)

private val ColorSurface         = Color(0xFF161B22)
private val ColorOnSurface       = Color(0xFFE6EDF3)

private val ColorSurfaceVariant  = Color(0xFF1C2128)
private val ColorOnSurfaceVariant = Color(0xFF8B949E)

private val ColorError           = Color(0xFFFF6B6B)
private val ColorOnError         = Color(0xFF690005)

private val ColorOutline         = Color(0xFF30363D)

private val PingToolDarkColorScheme = darkColorScheme(
    primary              = ColorPrimary,
    onPrimary            = ColorOnPrimary,
    primaryContainer     = ColorPrimaryContainer,
    onPrimaryContainer   = ColorOnPrimaryContainer,
    secondary            = ColorSecondary,
    onSecondary          = ColorOnSecondary,
    background           = ColorBackground,
    onBackground         = ColorOnBackground,
    surface              = ColorSurface,
    onSurface            = ColorOnSurface,
    surfaceVariant       = ColorSurfaceVariant,
    onSurfaceVariant     = ColorOnSurfaceVariant,
    error                = ColorError,
    onError              = ColorOnError,
    outline              = ColorOutline
)

@Composable
fun PingToolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PingToolDarkColorScheme,
        content = content
    )
}
