package com.pingmonitor.data

import com.pingmonitor.domain.NetworkDevice
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.AF_INET
import platform.posix.IPPROTO_UDP
import platform.posix.SOCK_DGRAM
import platform.posix.addrinfo
import platform.posix.close
import platform.posix.connect
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.socket
import kotlin.time.measureTimedValue

/**
 * iOS: el sandbox de Apple impide escanear la red local completa.
 * El escáner devuelve subred null y solo verifica conectividad UDP por host.
 */
@OptIn(ExperimentalForeignApi::class)
actual class NetworkScannerImpl actual constructor() : NetworkScannerRepository {

    // iOS no permite obtener la subred local desde la sandbox
    override fun getLocalSubnet(): String? = null

    override suspend fun ping(ip: String, timeoutMs: Int): NetworkDevice =
        withContext(Dispatchers.Default) {
            try {
                val (reachable, duration) = measureTimedValue { isReachable(ip) }
                NetworkDevice(
                    ip = ip,
                    hostname = null,
                    rttMs = if (reachable) duration.inWholeMilliseconds.toDouble() else null,
                    isReachable = reachable
                )
            } catch (_: Exception) {
                NetworkDevice(ip = ip, hostname = null, rttMs = null, isReachable = false)
            }
        }

    private fun isReachable(host: String): Boolean = memScoped {
        val hints = alloc<addrinfo>()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_DGRAM
        hints.ai_protocol = IPPROTO_UDP
        val result = allocPointerTo<addrinfo>()
        if (getaddrinfo(host, "7", hints.ptr, result.ptr) != 0) return@memScoped false
        val addr = result.value ?: return@memScoped false
        val fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        if (fd < 0) { freeaddrinfo(addr); return@memScoped false }
        val connected = connect(fd, addr.pointed.ai_addr, addr.pointed.ai_addrlen) == 0
        close(fd)
        freeaddrinfo(addr)
        connected
    }
}
