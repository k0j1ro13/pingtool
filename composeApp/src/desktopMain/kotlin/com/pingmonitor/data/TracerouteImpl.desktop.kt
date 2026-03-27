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
        val os = System.getProperty("os.name").lowercase()
        val isWindows = os.contains("win")

        val command = if (isWindows) {
            arrayOf("tracert", "-d", "-h", maxHops.toString(), "-w", "2000", host)
        } else {
            arrayOf("traceroute", "-n", "-m", maxHops.toString(), "-w", "2", host)
        }

        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        withContext(Dispatchers.IO) {
            process.inputStream.bufferedReader().forEachLine { line ->
                val hop = if (isWindows) parseWindows(line) else parseUnix(line)
                if (hop != null) trySend(hop)
            }
            process.waitFor()
        }

        close()
        awaitClose { process.destroyForcibly() }
    }

    // ── Windows: "  1    <1 ms     1 ms     1 ms  192.168.1.1"
    //             "  3     *        *        *     Request timed out."
    private val winLine = Regex("""^\s*(\d+)\s+(.+)$""")
    // acepta tanto "12 ms" como "<1 ms"
    private val winRtt  = Regex("""<?(\d+)\s*ms""")
    private val ip4     = Regex("""(\d{1,3}(?:\.\d{1,3}){3})""")

    private fun parseWindows(line: String): TracerouteHop? {
        val m   = winLine.find(line) ?: return null
        val ttl = m.groupValues[1].toIntOrNull() ?: return null
        val rest = m.groupValues[2]
        return if (rest.contains("*") || rest.contains("timed out", ignoreCase = true)) {
            TracerouteHop(ttl, null, null, null, HopStatus.TIMEOUT)
        } else {
            val rtt = winRtt.find(rest)?.groupValues?.get(1)?.toDoubleOrNull()
            val ip  = ip4.findAll(rest).lastOrNull()?.value
            TracerouteHop(ttl, ip, null, rtt, HopStatus.RESPONDED)
        }
    }

    // ── Unix: " 1  192.168.1.1  1.234 ms ..."
    //          " 3  * * *"
    private val unixLine = Regex("""^\s*(\d+)\s+(.+)$""")
    private val unixRtt  = Regex("""([\d.]+)\s*ms""")

    private fun parseUnix(line: String): TracerouteHop? {
        val m   = unixLine.find(line) ?: return null
        val ttl = m.groupValues[1].toIntOrNull() ?: return null
        val rest = m.groupValues[2].trim()
        return if (rest.startsWith("*")) {
            TracerouteHop(ttl, null, null, null, HopStatus.TIMEOUT)
        } else {
            val ip  = ip4.find(rest)?.value
            val rtt = unixRtt.find(rest)?.groupValues?.get(1)?.toDoubleOrNull()
            TracerouteHop(ttl, ip, null, rtt, HopStatus.RESPONDED)
        }
    }
}
