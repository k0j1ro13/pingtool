package com.pingmonitor.domain

/**
 * Calcula estadísticas acumuladas a partir de una lista de resultados de ping.
 */
object StatsCalculator {

    fun calculate(results: List<PingResult>): PingStats {
        if (results.isEmpty()) return PingStats.EMPTY

        val sent = results.size
        val received = results.count { it.status == PingStatus.OK }
        val lostPercent = if (sent == 0) 0.0 else (sent - received) * 100.0 / sent

        val rtts = results.mapNotNull { it.rttMs }

        return if (rtts.isEmpty()) {
            val score = calculateQualityScore(lostPercent, 0.0, 0.0)
            PingStats(
                sent = sent,
                received = received,
                lostPercent = lostPercent,
                rttMin = 0.0,
                rttAvg = 0.0,
                rttMax = 0.0,
                jitter = 0.0,
                qualityScore = score
            )
        } else {
            // Jitter: media de las diferencias absolutas entre RTTs consecutivos (RFC 3550)
            val jitter = if (rtts.size < 2) 0.0 else {
                rtts.zipWithNext { a, b -> kotlin.math.abs(b - a) }.average()
            }

            val rttAvg = rtts.average()
            val score = calculateQualityScore(lostPercent, rttAvg, jitter)

            PingStats(
                sent = sent,
                received = received,
                lostPercent = lostPercent,
                rttMin = rtts.min(),
                rttAvg = rttAvg,
                rttMax = rtts.max(),
                jitter = jitter,
                qualityScore = score
            )
        }
    }

    /**
     * Puntuación 0-100 ponderada:
     *  - 40 pts por RTT  (0 ms = 40, 300+ ms = 0)
     *  - 40 pts por pérdida (0% = 40, 100% = 0)
     *  - 20 pts por jitter (0 ms = 20, 100+ ms = 0)
     */
    private fun calculateQualityScore(lostPercent: Double, rttAvg: Double, jitter: Double): Int {
        val rttScore    = 40.0 * (1.0 - (rttAvg / 300.0)).coerceIn(0.0, 1.0)
        val lossScore   = 40.0 * (1.0 - lostPercent / 100.0).coerceIn(0.0, 1.0)
        val jitterScore = 20.0 * (1.0 - (jitter / 100.0)).coerceIn(0.0, 1.0)
        return (rttScore + lossScore + jitterScore).toInt().coerceIn(0, 100)
    }
}
