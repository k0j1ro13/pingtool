package com.pingmonitor.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class SpeedTesterImpl actual constructor() : SpeedTesterRepository {
    override suspend fun measurePing(host: String): Double? = null
    override fun measureDownload(): Flow<Double> = emptyFlow()
    override fun measureUpload(): Flow<Double> = emptyFlow()
}
