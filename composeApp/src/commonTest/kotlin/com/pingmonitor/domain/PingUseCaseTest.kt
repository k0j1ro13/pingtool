package com.pingmonitor.domain

import com.pingmonitor.data.PingerRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PingUseCaseTest {

    /** Repositorio falso que siempre responde OK con el RTT indicado */
    private fun fakeRepository(rttMs: Double = 15.0, status: PingStatus = PingStatus.OK) =
        object : PingerRepository {
            override suspend fun ping(host: String, sizeBytes: Int, timeoutMs: Int) = PingResult(
                seq = 0,
                sizeBytes = sizeBytes,
                rttMs = if (status == PingStatus.OK) rttMs else null,
                status = status,
                timestamp = 0L
            )
        }

    @Test
    fun `emite resultados con seq incremental`() = runTest {
        val useCase = PingUseCase(fakeRepository())

        val results = useCase.execute(
            host = "8.8.8.8",
            intervalMs = 0L,
            maxCount = 3
        ).toList()

        assertEquals(3, results.size)
        assertEquals(1, results[0].seq)
        assertEquals(2, results[1].seq)
        assertEquals(3, results[2].seq)
    }

    @Test
    fun `rota los tamaños de paquete`() = runTest {
        val sizes = listOf(32, 64, 128)
        val useCase = PingUseCase(fakeRepository())

        val results = useCase.execute(
            host = "8.8.8.8",
            intervalMs = 0L,
            sizes = sizes,
            maxCount = 6
        ).toList()

        assertEquals(32, results[0].sizeBytes)
        assertEquals(64, results[1].sizeBytes)
        assertEquals(128, results[2].sizeBytes)
        assertEquals(32, results[3].sizeBytes) // vuelve a rotar
    }

    @Test
    fun `respeta maxCount`() = runTest {
        val useCase = PingUseCase(fakeRepository())

        val results = useCase.execute(
            host = "1.1.1.1",
            intervalMs = 0L,
            maxCount = 5
        ).toList()

        assertEquals(5, results.size)
    }

    @Test
    fun `propaga status TIMEOUT del repositorio`() = runTest {
        val useCase = PingUseCase(fakeRepository(status = PingStatus.TIMEOUT))

        val result = useCase.execute(
            host = "192.168.1.1",
            intervalMs = 0L,
            maxCount = 1
        ).toList().first()

        assertEquals(PingStatus.TIMEOUT, result.status)
        assertEquals(null, result.rttMs)
    }

    @Test
    fun `emite al menos un resultado en modo infinito`() = runTest {
        val useCase = PingUseCase(fakeRepository())

        val result = useCase.execute(
            host = "8.8.8.8",
            intervalMs = 0L,
            maxCount = 0
        ).take(1).toList().firstOrNull()

        assertNotNull(result)
        assertEquals(1, result.seq)
    }
}
