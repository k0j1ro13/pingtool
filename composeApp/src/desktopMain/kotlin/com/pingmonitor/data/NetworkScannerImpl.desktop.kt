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
                val hostname = if (reachable) {
                    try {
                        address.canonicalHostName.takeIf { it != ip }
                    } catch (_: Exception) { null }
                } else null

                NetworkDevice(
                    ip = ip,
                    hostname = hostname,
                    rttMs = if (reachable) duration.inWholeMilliseconds.toDouble() else null,
                    isReachable = reachable
                )
            } catch (_: Exception) {
                NetworkDevice(ip = ip, hostname = null, rttMs = null, isReachable = false)
            }
        }
}
