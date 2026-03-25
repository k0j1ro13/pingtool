package com.pingmonitor.data

import com.pingmonitor.domain.NetworkDevice

interface NetworkScannerRepository {
    /** Devuelve el prefijo de la subred local (ej: "192.168.1"), o null si no se puede detectar. */
    fun getLocalSubnet(): String?

    /** Comprueba si la IP es accesible y devuelve el resultado. */
    suspend fun ping(ip: String, timeoutMs: Int): NetworkDevice
}
