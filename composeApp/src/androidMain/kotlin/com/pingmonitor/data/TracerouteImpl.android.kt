package com.pingmonitor.data

import com.pingmonitor.domain.HopStatus
import com.pingmonitor.domain.TracerouteHop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

actual class TracerouteImpl actual constructor() : TracerouteRepository {

    override fun trace(host: String, maxHops: Int): Flow<TracerouteHop> = callbackFlow {
        try {
            val process = ProcessBuilder("traceroute", "-n", "-m", maxHops.toString(), "-w", "2", host)
                .redirectErrorStream(true)
                .start()

            val hopLine = Regex("""^\s*(\d+)\s+(.+)$""")
            val ipRegex = Regex("""(\d{1,3}(?:\.\d{1,3}){3})""")
            val rttRegex = Regex("""([\d.]+)\s*ms""")

            withContext(Dispatchers.IO) {
                process.inputStream.bufferedReader().forEachLine { line ->
                    val m = hopLine.find(line) ?: return@forEachLine
                    val ttl = m.groupValues[1].toIntOrNull() ?: return@forEachLine
                    val rest = m.groupValues[2].trim()
                    val hop = if (rest.startsWith("*")) {
                        TracerouteHop(ttl, null, null, null, HopStatus.TIMEOUT)
                    } else {
                        val ip  = ipRegex.find(rest)?.value
                        val rtt = rttRegex.find(rest)?.groupValues?.get(1)?.toDoubleOrNull()
                        TracerouteHop(ttl, ip, null, rtt, HopStatus.RESPONDED)
                    }
                    trySend(hop)
                }
                process.waitFor()
            }
        } catch (_: Exception) {
            trySend(TracerouteHop(0, null, null, null, HopStatus.ERROR))
        }

        close()
        awaitClose()
    }
}
