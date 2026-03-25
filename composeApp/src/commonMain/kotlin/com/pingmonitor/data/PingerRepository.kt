package com.pingmonitor.data

import com.pingmonitor.domain.PingResult

/**
 * Contrato que deben cumplir todas las implementaciones de ping por plataforma.
 */
interface PingerRepository {
    /**
     * Envía un paquete ICMP al [host] con el tamaño [sizeBytes] y espera respuesta
     * hasta [timeoutMs] milisegundos.
     *
     * @return [PingResult] con el RTT medido o estado TIMEOUT/ERROR.
     */
    suspend fun ping(host: String, sizeBytes: Int, timeoutMs: Int = 2000): PingResult
}
