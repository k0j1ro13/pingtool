package com.pingmonitor.data

import com.pingmonitor.domain.TracerouteHop
import kotlinx.coroutines.flow.Flow

interface TracerouteRepository {
    fun trace(host: String, maxHops: Int = 30): Flow<TracerouteHop>
}
