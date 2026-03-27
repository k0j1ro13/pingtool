package com.pingmonitor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta oscura — estética de herramienta de red profesional ──────────────
private val DarkPrimary              = Color(0xFF00BFA5)
private val DarkOnPrimary            = Color(0xFF003731)
private val DarkPrimaryContainer     = Color(0xFF004D45)
private val DarkOnPrimaryContainer   = Color(0xFF70F7E6)
private val DarkSecondary            = Color(0xFF0097A7)
private val DarkOnSecondary          = Color(0xFF001F24)
private val DarkBackground           = Color(0xFF0D1117)
private val DarkOnBackground         = Color(0xFFE6EDF3)
private val DarkSurface              = Color(0xFF161B22)
private val DarkOnSurface            = Color(0xFFE6EDF3)
private val DarkSurfaceVariant       = Color(0xFF1C2128)
private val DarkOnSurfaceVariant     = Color(0xFF8B949E)
private val DarkError                = Color(0xFFFF6B6B)
private val DarkOnError              = Color(0xFF690005)
private val DarkOutline              = Color(0xFF30363D)

private val PingToolDarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryContainer,
    onPrimaryContainer   = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    error                = DarkError,
    onError              = DarkOnError,
    outline              = DarkOutline
)

// ── Paleta clara ─────────────────────────────────────────────────────────────
private val PingToolLightColorScheme = lightColorScheme(
    primary              = Color(0xFF006B5F),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF9EF2E4),
    onPrimaryContainer   = Color(0xFF00201B),
    secondary            = Color(0xFF00677A),
    onSecondary          = Color(0xFFFFFFFF),
    background           = Color(0xFFF4FBFA),
    onBackground         = Color(0xFF171D1D),
    surface              = Color(0xFFF4FBFA),
    onSurface            = Color(0xFF171D1D),
    surfaceVariant       = Color(0xFFDAE5E3),
    onSurfaceVariant     = Color(0xFF3F4948),
    error                = Color(0xFFBA1A1A),
    onError              = Color(0xFFFFFFFF),
    outline              = Color(0xFF6F7978)
)

@Composable
fun PingToolTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) PingToolDarkColorScheme else PingToolLightColorScheme,
        content = content
    )
}
