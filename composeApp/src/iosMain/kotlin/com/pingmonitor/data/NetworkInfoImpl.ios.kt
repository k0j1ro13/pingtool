package com.pingmonitor.data

import com.pingmonitor.domain.NetworkInfo

/** iOS: el sandbox restringe el acceso a información de red detallada. */
actual class NetworkInfoImpl actual constructor() : NetworkInfoRepository {
    override suspend fun getNetworkInfo(): NetworkInfo? = null
}
