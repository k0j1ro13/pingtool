package com.pingmonitor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Paleta oscura mejorada ───────────────────────────────────────────────────
private val DarkPrimary              = Color(0xFF26D0BE)   // teal vivo
private val DarkOnPrimary            = Color(0xFF00201C)
private val DarkPrimaryContainer     = Color(0xFF004D44)
private val DarkOnPrimaryContainer   = Color(0xFF80F5E4)
private val DarkSecondary            = Color(0xFF00BCD4)   // cyan
private val DarkOnSecondary          = Color(0xFF00212A)
private val DarkBackground           = Color(0xFF090D12)   // negro profundo
private val DarkOnBackground         = Color(0xFFE1EAF2)   // blanco azulado
private val DarkSurface              = Color(0xFF10161D)   // superficie oscura
private val DarkOnSurface            = Color(0xFFE1EAF2)
private val DarkSurfaceVariant       = Color(0xFF182028)   // tarjetas
private val DarkOnSurfaceVariant     = Color(0xFF8FA8BE)   // texto secundario
private val DarkError                = Color(0xFFFF6B6B)
private val DarkOnError              = Color(0xFF690005)
private val DarkOutline              = Color(0xFF2A3A46)   // bordes sutiles

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

// ── Paleta clara (principal) ─────────────────────────────────────────────────
private val PingToolLightColorScheme = lightColorScheme(
    primary              = Color(0xFF006B5F),   // teal oscuro
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFB2DFDB),   // mint suave
    onPrimaryContainer   = Color(0xFF00201B),
    secondary            = Color(0xFF00677A),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF001F26),
    background           = Color(0xFFF6FFFE),   // blanco-teal muy suave
    onBackground         = Color(0xFF141D1D),
    surface              = Color(0xFFFFFFFF),   // blanco puro para tarjetas
    onSurface            = Color(0xFF141D1D),
    surfaceVariant       = Color(0xFFE4F2F0),   // fondo de tarjetas
    onSurfaceVariant     = Color(0xFF3E4948),
    error                = Color(0xFFBA1A1A),
    onError              = Color(0xFFFFFFFF),
    outline              = Color(0xFF6F7978)
)

@Composable
fun PingToolTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) PingToolDarkColorScheme else PingToolLightColorScheme,
        content = content
    )
}
