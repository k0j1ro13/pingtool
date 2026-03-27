package com.pingmonitor.data

import com.pingmonitor.domain.NetworkDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.time.measureTimedValue

actual class NetworkScannerImpl actual constructor() : NetworkScannerRepository {

    override fun getLocalSubnet(): String? = try {
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.filter { !it.isLoopback && it.isUp }
            ?.flatMap { ni ->
                ni.interfaceAddresses
                    .filter { it.address is Inet4Address }
                    .map { it.address.hostAddress }
            }
            ?.firstOrNull()
            ?.substringBeforeLast(".")
    } catch (_: Exception) { null }

    override suspend fun ping(ip: String, timeoutMs: Int): NetworkDevice =
        withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(ip)
                val (reachable, duration) = measureTimedValue {
                    address.isReachable(timeoutMs)
                }
                NetworkDevice(
                    ip         = ip,
                    hostname   = if (reachable) resolveHostname(ip) else null,
                    rttMs      = if (reachable) duration.inWholeMilliseconds.toDouble() else null,
                    isReachable = reachable
                )
            } catch (_: Exception) {
                NetworkDevice(ip = ip, hostname = null, rttMs = null, isReachable = false)
            }
        }

    /**
     * Extrae el hostname desde la primera línea del comando ping:
     *   "PING hostname (ip) ..." → devuelve "hostname"
     *   "PING ip (ip) ..."       → devuelve null
     * Usa el resolver completo del sistema (DNS + mDNS), más fiable que canonicalHostName.
     */
    private fun resolveHostname(ip: String): String? = try {
        val process = ProcessBuilder("ping", "-c", "1", "-W", "1", ip)
            .redirectErrorStream(true)
            .start()
        val firstLine = process.inputStream.bufferedReader().readLine() ?: return null
        process.destroyForcibly()
        // "PING hostname (ip) 56(84) bytes of data."
        val match = Regex("""^PING\s+(\S+)\s+\(""").find(firstLine)
        match?.groupValues?.get(1)?.takeIf { it != ip }
    } catch (_: Exception) { null }
}
