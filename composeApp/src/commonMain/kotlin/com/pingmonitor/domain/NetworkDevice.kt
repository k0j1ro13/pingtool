package com.pingmonitor.domain

/** Dispositivo descubierto durante un escaneo de red. */
data class NetworkDevice(
    val ip: String,
    val hostname: String?,   // null si no se puede resolver
    val rttMs: Double?,
    val isReachable: Boolean
)
