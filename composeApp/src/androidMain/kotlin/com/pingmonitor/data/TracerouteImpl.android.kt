package com.pingmonitor.data

import com.pingmonitor.domain.HopStatus
import com.pingmonitor.domain.TracerouteHop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

actual class TracerouteImpl actual constructor() : TracerouteRepository {

    // "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=12.3 ms"
    private val successRegex = Regex("""\d+ bytes from ([\d.]+).*time=([\d.]+)\s*ms""")

    // "From 192.168.1.1 icmp_seq=1 Time to live exceeded"
    private val ttlExceededRegex = Regex("""From ([\d.]+).*[Tt]ime to live exceeded""")

    override fun trace(host: String, maxHops: Int): Flow<TracerouteHop> = callbackFlow {
        withContext(Dispatchers.IO) {
            for (ttl in 1..maxHops) {
                val start = System.currentTimeMillis()
                try {
                    val process = ProcessBuilder(
                        "ping", "-c", "1", "-W", "2", "-t", ttl.toString(), host
                    )
                        .redirectErrorStream(true)
                        .start()
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    val elapsed = (System.currentTimeMillis() - start).toDouble()

                    val successMatch = successRegex.find(output)
                    if (successMatch != null) {
                        val ip  = successMatch.groupValues[1]
                        val rtt = successMatch.groupValues[2].toDoubleOrNull()
                        trySend(TracerouteHop(ttl, ip, null, rtt, HopStatus.RESPONDED))
                        break // destino alcanzado
                    }

                    val ttlMatch = ttlExceededRegex.find(output)
                    if (ttlMatch != null) {
                        val ip = ttlMatch.groupValues[1]
                        trySend(TracerouteHop(ttl, ip, null, elapsed, HopStatus.RESPONDED))
                    } else {
                        trySend(TracerouteHop(ttl, null, null, null, HopStatus.TIMEOUT))
                    }
                } catch (_: Exception) {
                    trySend(TracerouteHop(ttl, null, null, null, HopStatus.ERROR))
                    break
                }
            }
        }
        close()
        awaitClose()
    }
}
