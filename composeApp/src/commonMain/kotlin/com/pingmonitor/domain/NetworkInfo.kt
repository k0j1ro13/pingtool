package com.pingmonitor.domain

enum class ConnectionType { WIFI, ETHERNET, LOOPBACK, UNKNOWN }

data class NetworkInfo(
    val interfaceName: String,
    val connectionType: ConnectionType,
    // Direccionamiento
    val localIp: String?,
    val subnetMask: String?,
    val gateway: String?,
    val ipv6: String?,
    val publicIp: String?,
    // DNS
    val dnsServers: List<String>,
    // Identificación
    val macAddress: String?,
    val mtu: Int?,
    // WiFi (solo si connectionType == WIFI)
    val ssid: String?,
    val signalPercent: Int?,   // 0–100
    val signalDbm: Int?        // valor negativo en dBm, ej. -65
)
