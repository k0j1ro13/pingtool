package com.pingmonitor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsCalculatorTest {

    @Test
    fun `lista vacía devuelve estadísticas en cero`() {
        val stats = StatsCalculator.calculate(emptyList())
        assertEquals(PingStats.EMPTY, stats)
    }

    @Test
    fun `todos OK calcula RTT correctamente`() {
        val results = listOf(
            result(seq = 1, rttMs = 10.0, status = PingStatus.OK),
            result(seq = 2, rttMs = 20.0, status = PingStatus.OK),
            result(seq = 3, rttMs = 30.0, status = PingStatus.OK)
        )
        val stats = StatsCalculator.calculate(results)

        assertEquals(3, stats.sent)
        assertEquals(3, stats.received)
        assertEquals(0.0, stats.lostPercent)
        assertEquals(10.0, stats.rttMin)
        assertEquals(20.0, stats.rttAvg)
        assertEquals(30.0, stats.rttMax)
    }

    @Test
    fun `timeout cuenta como perdido y no afecta RTT`() {
        val results = listOf(
            result(seq = 1, rttMs = 10.0, status = PingStatus.OK),
            result(seq = 2, rttMs = null, status = PingStatus.TIMEOUT),
            result(seq = 3, rttMs = 30.0, status = PingStatus.OK)
        )
        val stats = StatsCalculator.calculate(results)

        assertEquals(3, stats.sent)
        assertEquals(2, stats.received)
        assertTrue(stats.lostPercent > 0.0)
        assertEquals(10.0, stats.rttMin)
        assertEquals(30.0, stats.rttMax)
    }

    @Test
    fun `todos timeout devuelve RTT en cero`() {
        val results = listOf(
            result(seq = 1, rttMs = null, status = PingStatus.TIMEOUT),
            result(seq = 2, rttMs = null, status = PingStatus.TIMEOUT)
        )
        val stats = StatsCalculator.calculate(results)

        assertEquals(2, stats.sent)
        assertEquals(0, stats.received)
        assertEquals(100.0, stats.lostPercent)
        assertEquals(0.0, stats.rttMin)
        assertEquals(0.0, stats.rttAvg)
        assertEquals(0.0, stats.rttMax)
    }

    @Test
    fun `error cuenta como perdido`() {
        val results = listOf(
            result(seq = 1, rttMs = null, status = PingStatus.ERROR),
            result(seq = 2, rttMs = 15.0, status = PingStatus.OK)
        )
        val stats = StatsCalculator.calculate(results)

        assertEquals(2, stats.sent)
        assertEquals(1, stats.received)
        assertEquals(50.0, stats.lostPercent)
    }

    // Función auxiliar para crear resultados de prueba
    private fun result(seq: Int, rttMs: Double?, status: PingStatus) = PingResult(
        seq = seq,
        sizeBytes = 64,
        rttMs = rttMs,
        status = status,
        timestamp = 0L
    )
}
