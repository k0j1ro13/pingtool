package com.pingmonitor.data

import kotlinx.coroutines.flow.Flow

interface SpeedTesterRepository {
    suspend fun measurePing(host: String): Double?
    fun measureDownload(): Flow<Double>
    fun measureUpload(): Flow<Double>
}
