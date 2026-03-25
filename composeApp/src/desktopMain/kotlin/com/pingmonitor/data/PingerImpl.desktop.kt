package com.pingmonitor.data

import com.pingmonitor.domain.PingResult
import com.pingmonitor.domain.PingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.measureTimedValue

/**
 * Implementación Desktop (JVM) que invoca el comando ping del sistema operativo.
 * Soporta Windows y Unix/macOS. Permite controlar el tamaño real del paquete.
 */
actual class PingerImpl actual constructor() : PingerRepository {

    override suspend fun ping(host: String, sizeBytes: Int, timeoutMs: Int): PingResult =
        withContext(Dispatchers.IO) {
            try {
                val isWindows = System.getProperty("os.name").lowercase().contains("windows")
                val timeoutSec = (timeoutMs / 1000).coerceAtLeast(1)

                // Comando ping según el SO
                val command = if (isWindows) {
                    listOf("ping", "-n", "1", "-l", sizeBytes.toString(), "-w", timeoutMs.toString(), host)
                } else {
                    listOf("ping", "-c", "1", "-s", sizeBytes.toString(), "-W", timeoutSec.toString(), host)
                }

                val (process, duration) = measureTimedValue {
                    ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start()
                        .also { it.waitFor() }
                }

                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.exitValue()

                if (exitCode == 0) {
                    // Intentar extraer RTT real de la salida del comando
                    val rtt = parseRttFromOutput(output, isWindows) ?: duration.inWholeMilliseconds.toDouble()
                    PingResult(
                        seq = 0,
                        sizeBytes = sizeBytes,
                        rttMs = rtt,
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

    /**
     * Extrae el RTT en ms de la salida del comando ping.
     * Windows: "tiempo=12ms" o "time=12ms"
     * Unix:    "time=12.4 ms"
     */
    private fun parseRttFromOutput(output: String, isWindows: Boolean): Double? {
        return if (isWindows) {
            Regex("(?:tiempo|time)[=<](\\d+)ms", RegexOption.IGNORE_CASE)
                .find(output)?.groupValues?.get(1)?.toDoubleOrNull()
        } else {
            Regex("time=(\\d+\\.?\\d*)\\s*ms", RegexOption.IGNORE_CASE)
                .find(output)?.groupValues?.get(1)?.toDoubleOrNull()
        }
    }
}
