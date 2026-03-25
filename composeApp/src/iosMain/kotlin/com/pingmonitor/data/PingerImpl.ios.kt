package com.pingmonitor.data

import com.pingmonitor.domain.PingResult
import com.pingmonitor.domain.PingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.posix.getaddrinfo
import platform.posix.freeaddrinfo
import platform.posix.addrinfo
import platform.posix.AF_INET
import platform.posix.SOCK_DGRAM
import platform.posix.IPPROTO_UDP
import platform.posix.socket
import platform.posix.connect
import platform.posix.close
import kotlinx.cinterop.*

/**
 * Implementación iOS: usa un socket UDP hacia el puerto 7 (echo) para medir
 * la alcanzabilidad del host. Para ICMP real se necesitaría entitlement especial.
 *
 * Nota: iOS restringe los raw sockets a apps con entitlement com.apple.developer.networking.
 * Esta implementación usa el tiempo de resolución DNS + conexión UDP como aproximación.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PingerImpl actual constructor() : PingerRepository {

    override suspend fun ping(host: String, sizeBytes: Int, timeoutMs: Int): PingResult =
        withContext(Dispatchers.Default) {
            val startMs = (NSDate().timeIntervalSince1970 * 1000).toLong()
            try {
                val reachable = isReachable(host, timeoutMs)
                val endMs = (NSDate().timeIntervalSince1970 * 1000).toLong()
                val rttMs = (endMs - startMs).toDouble()

                if (reachable) {
                    PingResult(
                        seq = 0,
                        sizeBytes = sizeBytes,
                        rttMs = rttMs,
                        status = PingStatus.OK,
                        timestamp = startMs
                    )
                } else {
                    PingResult(
                        seq = 0,
                        sizeBytes = sizeBytes,
                        rttMs = null,
                        status = PingStatus.TIMEOUT,
                        timestamp = startMs
                    )
                }
            } catch (e: Exception) {
                PingResult(
                    seq = 0,
                    sizeBytes = sizeBytes,
                    rttMs = null,
                    status = PingStatus.ERROR,
                    timestamp = startMs
                )
            }
        }

    /**
     * Intenta resolver el host y abrir un socket UDP para comprobar alcanzabilidad.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun isReachable(host: String, timeoutMs: Int): Boolean {
        return memScoped {
            val hints = alloc<addrinfo>()
            hints.ai_family = AF_INET
            hints.ai_socktype = SOCK_DGRAM
            hints.ai_protocol = IPPROTO_UDP

            val result = allocPointerTo<addrinfo>()
            val status = getaddrinfo(host, "7", hints.ptr, result.ptr)
            if (status != 0) return@memScoped false

            val addr = result.value ?: return@memScoped false
            val fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
            if (fd < 0) {
                freeaddrinfo(addr)
                return@memScoped false
            }

            val connected = connect(fd, addr.pointed.ai_addr, addr.pointed.ai_addrlen) == 0
            close(fd)
            freeaddrinfo(addr)
            connected
        }
    }
}
