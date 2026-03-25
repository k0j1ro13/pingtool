package com.pingmonitor.domain

enum class SpeedTestPhase { IDLE, PING, DOWNLOAD, UPLOAD, DONE, ERROR }

data class SpeedTestProgress(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val pingMs: Double? = null,
    val isDone: Boolean = false,
    val error: String? = null
)
