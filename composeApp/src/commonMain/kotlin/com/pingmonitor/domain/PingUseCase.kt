package com.pingmonitor.domain

import com.pingmonitor.data.PingerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Caso de uso: ping continuo a un host con rotación de tamaños de paquete.
 *
 * Emite un [PingResult] por cada paquete enviado hasta que el Flow sea cancelado.
 */
class PingUseCase(private val repository: PingerRepository) {

    // Tamaños de paquete por defecto en bytes
    private val defaultSizes = listOf(32, 64, 128, 256, 512, 1024)

    /**
     * @param host        IP o hostname destino.
     * @param intervalMs  Milisegundos entre pings.
     * @param sizes       Lista de tamaños de paquete a rotar.
     * @param timeoutMs   Tiempo máximo de espera por respuesta.
     * @param maxCount    Número máximo de pings (0 = infinito).
     */
    fun execute(
        host: String,
        intervalMs: Long = 1000L,
        sizes: List<Int> = defaultSizes,
        timeoutMs: Int = 2000,
        maxCount: Int = 0
    ): Flow<PingResult> = flow {
        var seq = 1
        val sizeList = sizes.ifEmpty { defaultSizes }

        while (maxCount == 0 || seq <= maxCount) {
            val sizeBytes = sizeList[(seq - 1) % sizeList.size]
            val result = repository.ping(host, sizeBytes, timeoutMs).copy(seq = seq)
            emit(result)
            seq++
            if (maxCount == 0 || seq <= maxCount) {
                delay(intervalMs)
            }
        }
    }
}
