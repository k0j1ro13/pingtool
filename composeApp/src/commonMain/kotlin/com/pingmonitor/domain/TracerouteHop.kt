package com.pingmonitor.domain

data class TracerouteHop(
    val ttl: Int,
    val ip: String?,
    val hostname: String?,
    val rttMs: Double?,
    val status: HopStatus
)

enum class HopStatus { RESPONDED, TIMEOUT, ERROR }
