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
            PingStats(
                sent = sent,
                received = received,
                lostPercent = lostPercent,
                rttMin = 0.0,
                rttAvg = 0.0,
                rttMax = 0.0
            )
        } else {
            PingStats(
                sent = sent,
                received = received,
                lostPercent = lostPercent,
                rttMin = rtts.min(),
                rttAvg = rtts.average(),
                rttMax = rtts.max()
            )
        }
    }
}
