package com.pingmonitor.data

import com.pingmonitor.domain.HopStatus
import com.pingmonitor.domain.TracerouteHop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class TracerouteImpl actual constructor() : TracerouteRepository {
    override fun trace(host: String, maxHops: Int): Flow<TracerouteHop> =
        flowOf(TracerouteHop(0, null, null, null, HopStatus.ERROR))
}
