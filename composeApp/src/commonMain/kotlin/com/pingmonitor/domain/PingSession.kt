package com.pingmonitor.domain

/**
 * Resumen de una sesión de ping completada.
 * Se guarda en el historial cuando el usuario detiene el ping.
 */
data class PingSession(
    val id: Long = System.currentTimeMillis(),
    val host: String,
    val startMs: Long,
    val endMs: Long,
    val stats: PingStats
) {
    val durationMs: Long get() = endMs - startMs
}
