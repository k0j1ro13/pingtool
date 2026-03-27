package com.pingmonitor.data

import com.pingmonitor.domain.PingResult
import com.pingmonitor.domain.PingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import kotlin.time.measureTimedValue

/**
 * Implementación Android usando InetAddress.isReachable().
 * Requiere permiso INTERNET en el AndroidManifest.
 */
actual class PingerImpl actual constructor() : PingerRepository {

    override suspend fun resolveHost(host: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val addr = InetAddress.getByName(host)
                addr.hostAddress?.takeIf { it != host }
            } catch (e: Exception) { null }
        }

    override suspend fun ping(host: String, sizeBytes: Int, timeoutMs: Int): PingResult =
        withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(host)
                val (reachable, duration) = measureTimedValue {
                    address.isReachable(timeoutMs)
                }
                val rttMs = duration.inWholeMilliseconds.toDouble()

                if (reachable) {
                    PingResult(
                        seq = 0, // el seq lo asigna PingUseCase
                        sizeBytes = sizeBytes,
                        rttMs = rttMs,
                        status = PingStatus.OK,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    PingResult(
                        seq = 0,
                        sizeBytes = sizeBytes,
                        rttMs = null,
                        status = PingStatus.TIMEOUT,
                        timestamp = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                PingResult(
                    seq = 0,
                    sizeBytes = sizeBytes,
                    rttMs = null,
                    status = PingStatus.ERROR,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
}
