package com.pingmonitor.domain

import com.pingmonitor.data.SpeedTesterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SpeedTestUseCase(private val repository: SpeedTesterRepository) {

    fun execute(): Flow<SpeedTestProgress> = flow {
        // ── Fase 1: Ping ─────────────────────────────────────────────────────
        emit(SpeedTestProgress(phase = SpeedTestPhase.PING))
        val pingMs = try {
            repository.measurePing("1.1.1.1")
        } catch (e: Exception) {
            null
        }

        // ── Fase 2: Descarga ─────────────────────────────────────────────────
        emit(SpeedTestProgress(phase = SpeedTestPhase.DOWNLOAD, pingMs = pingMs))
        var lastDownload = 0.0
        try {
            repository.measureDownload().collect { mbps ->
                lastDownload = mbps
                emit(SpeedTestProgress(
                    phase = SpeedTestPhase.DOWNLOAD,
                    downloadMbps = mbps,
                    pingMs = pingMs
                ))
            }
        } catch (e: Exception) {
            emit(SpeedTestProgress(phase = SpeedTestPhase.ERROR, error = "Error de descarga: ${e.message}"))
            return@flow
        }

        // ── Fase 3: Subida ───────────────────────────────────────────────────
        emit(SpeedTestProgress(
            phase = SpeedTestPhase.UPLOAD,
            downloadMbps = lastDownload,
            pingMs = pingMs
        ))
        var lastUpload = 0.0
        try {
            repository.measureUpload().collect { mbps ->
                lastUpload = mbps
                emit(SpeedTestProgress(
                    phase = SpeedTestPhase.UPLOAD,
                    downloadMbps = lastDownload,
                    uploadMbps = mbps,
                    pingMs = pingMs
                ))
            }
        } catch (e: Exception) {
            // Si la subida falla, mostramos el resultado parcial igualmente
        }

        // ── Resultado final ──────────────────────────────────────────────────
        emit(SpeedTestProgress(
            phase = SpeedTestPhase.DONE,
            downloadMbps = lastDownload,
            uploadMbps = lastUpload,
            pingMs = pingMs,
            isDone = true
        ))
    }
}
