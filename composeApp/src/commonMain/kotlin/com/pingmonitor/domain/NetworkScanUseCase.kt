package com.pingmonitor.domain

import com.pingmonitor.data.NetworkScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Escanea todos los hosts de una subred /24 en paralelo.
 * Emite cada dispositivo (accesible o no) según se va completando.
 */
class NetworkScanUseCase(private val repository: NetworkScannerRepository) {

    fun getLocalSubnet(): String? = repository.getLocalSubnet()

    /**
     * @param subnet     Prefijo de red, ej. "192.168.1"
     * @param timeoutMs  Timeout por host en ms
     * @param concurrency Número de hosts comprobados simultáneamente
     */
    fun scan(
        subnet: String,
        timeoutMs: Int = 1500,
        concurrency: Int = 40
    ): Flow<NetworkDevice> = channelFlow {
        val semaphore = Semaphore(concurrency)
        val jobs = (1..254).map { i ->
            launch {
                semaphore.withPermit {
                    val device = repository.ping("$subnet.$i", timeoutMs)
                    send(device)
                }
            }
        }
        jobs.forEach { it.join() }
    }
}
