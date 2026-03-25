package com.pingmonitor.domain

/**
 * Resultado de un único paquete de ping.
 *
 * @param seq       Número de secuencia del paquete (empieza en 1).
 * @param sizeBytes Tamaño del paquete enviado en bytes.
 * @param rttMs     Tiempo de ida y vuelta en milisegundos. Null si hubo timeout o error.
 * @param status    Estado del ping: OK, TIMEOUT o ERROR.
 * @param timestamp Marca de tiempo Unix (ms) en que se envió el paquete.
 */
data class PingResult(
    val seq: Int,
    val sizeBytes: Int,
    val rttMs: Double?,
    val status: PingStatus,
    val timestamp: Long
)

enum class PingStatus { OK, TIMEOUT, ERROR }

/**
 * Estadísticas acumuladas de una sesión de ping.
 */
data class PingStats(
    val sent: Int,
    val received: Int,
    val lostPercent: Double,
    val rttMin: Double,
    val rttAvg: Double,
    val rttMax: Double
) {
    companion object {
        val EMPTY = PingStats(
            sent = 0,
            received = 0,
            lostPercent = 0.0,
            rttMin = 0.0,
            rttAvg = 0.0,
            rttMax = 0.0
        )
    }
}
