package com.pingmonitor.data

import com.pingmonitor.domain.NetworkInfo

interface NetworkInfoRepository {
    suspend fun getNetworkInfo(): NetworkInfo?
    suspend fun resolveHostname(ip: String): String?
}
