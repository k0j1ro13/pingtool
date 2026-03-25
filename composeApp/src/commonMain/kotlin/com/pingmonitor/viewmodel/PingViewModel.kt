package com.pingmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingmonitor.data.NotifierRepository
import com.pingmonitor.domain.PingResult
import com.pingmonitor.domain.PingSession
import com.pingmonitor.domain.PingStats
import com.pingmonitor.domain.PingStatus
import com.pingmonitor.domain.PingUseCase
import com.pingmonitor.domain.StatsCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Máximo de resultados guardados en memoria para evitar fugas
private const val MAX_RESULTS = 200

// Umbrales para notificaciones
private const val TIMEOUT_STREAK_THRESHOLD = 3    // timeouts consecutivos
private const val HIGH_LOSS_THRESHOLD      = 20.0 // % de pérdida
private const val LOSS_RECOVERY_THRESHOLD  = 5.0  // % para considerar recuperado
private const val NOTIF_ID_TIMEOUT         = 1001
private const val NOTIF_ID_HIGH_LOSS       = 1002
private const val NOTIF_ID_RECOVERED       = 1003

data class PingUiState(
    val host: String = "",
    val intervalMs: Long = 1000L,
    val selectedSizeBytes: Int? = null,
    val results: List<PingResult> = emptyList(),
    val stats: PingStats = PingStats.EMPTY,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val errorMessage: String? = null,
    val sessionStartMs: Long? = null,
    val sessionHistory: List<PingSession> = emptyList()
)

class PingViewModel(
    private val pingUseCase: PingUseCase,
    private val notifier: NotifierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PingUiState())
    val uiState: StateFlow<PingUiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null

    // Seguimiento para notificaciones (no forman parte del estado de UI)
    private var consecutiveTimeouts = 0
    private var hasNotifiedHighLoss = false
    private var wasHighLoss         = false

    fun onHostChange(host: String) {
        _uiState.update { it.copy(host = host, errorMessage = null) }
    }

    fun onIntervalChange(intervalMs: Long) {
        _uiState.update { it.copy(intervalMs = intervalMs) }
    }

    fun onSizeChange(sizeBytes: Int?) {
        _uiState.update { it.copy(selectedSizeBytes = sizeBytes) }
    }

    fun startPing() {
        val host = _uiState.value.host.trim()
        if (host.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Introduce una IP o hostname válido") }
            return
        }

        pingJob?.cancel()
        val updatedHistory = saveCurrentSession(_uiState.value)
        resetNotificationTrackers()

        _uiState.update {
            it.copy(
                results        = emptyList(),
                stats          = PingStats.EMPTY,
                isRunning      = true,
                isPaused       = false,
                errorMessage   = null,
                sessionStartMs = System.currentTimeMillis(),
                sessionHistory = updatedHistory
            )
        }
        launchPing(host)
    }

    fun pausePing() {
        pingJob?.cancel()
        pingJob = null
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resumePing() {
        val host = _uiState.value.host.trim()
        if (host.isEmpty()) return
        _uiState.update { it.copy(isRunning = true, isPaused = false, errorMessage = null) }
        launchPing(host)
    }

    fun stopPing() {
        pingJob?.cancel()
        pingJob = null
        val updatedHistory = saveCurrentSession(_uiState.value)
        _uiState.update {
            it.copy(
                isRunning      = false,
                isPaused       = false,
                sessionStartMs = null,
                sessionHistory = updatedHistory
            )
        }
    }

    fun clearResults() {
        _uiState.update { it.copy(results = emptyList(), stats = PingStats.EMPTY) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(sessionHistory = emptyList()) }
    }

    private fun saveCurrentSession(state: PingUiState): List<PingSession> {
        val startMs = state.sessionStartMs
        return if (state.results.isNotEmpty() && startMs != null) {
            val session = PingSession(
                host    = state.host,
                startMs = startMs,
                endMs   = System.currentTimeMillis(),
                stats   = state.stats
            )
            state.sessionHistory + session
        } else {
            state.sessionHistory
        }
    }

    private fun resetNotificationTrackers() {
        consecutiveTimeouts = 0
        hasNotifiedHighLoss = false
        wasHighLoss         = false
    }

    private fun checkNotifications(result: PingResult, stats: PingStats) {
        // Timeouts consecutivos
        if (result.status == PingStatus.TIMEOUT || result.status == PingStatus.ERROR) {
            consecutiveTimeouts++
            if (consecutiveTimeouts == TIMEOUT_STREAK_THRESHOLD) {
                notifier.notify(
                    title          = "Sin respuesta",
                    message        = "El host ${_uiState.value.host} no responde (${consecutiveTimeouts} timeouts seguidos)",
                    notificationId = NOTIF_ID_TIMEOUT
                )
            }
        } else {
            // Recuperación tras pérdida alta
            if (wasHighLoss && stats.lostPercent < LOSS_RECOVERY_THRESHOLD) {
                wasHighLoss = false
                hasNotifiedHighLoss = false
                notifier.notify(
                    title          = "Conexión recuperada",
                    message        = "La pérdida de paquetes ha vuelto a niveles normales",
                    notificationId = NOTIF_ID_RECOVERED
                )
            }
            consecutiveTimeouts = 0
        }

        // Alta pérdida (notificar solo una vez por sesión hasta que se recupere)
        if (!hasNotifiedHighLoss && stats.sent >= 10 && stats.lostPercent >= HIGH_LOSS_THRESHOLD) {
            hasNotifiedHighLoss = true
            wasHighLoss         = true
            notifier.notify(
                title          = "Alta pérdida de paquetes",
                message        = "Se está perdiendo el ${"%.0f".format(stats.lostPercent)}% de los paquetes",
                notificationId = NOTIF_ID_HIGH_LOSS
            )
        }
    }

    private fun launchPing(host: String) {
        val sizes = _uiState.value.selectedSizeBytes?.let { listOf(it) }
        pingJob = viewModelScope.launch {
            pingUseCase.execute(
                host       = host,
                intervalMs = _uiState.value.intervalMs,
                sizes      = sizes ?: listOf(32, 64, 128, 256, 512, 1024)
            ).collect { result ->
                _uiState.update { state ->
                    val updatedResults = (state.results + result).takeLast(MAX_RESULTS)
                    val updatedStats   = StatsCalculator.calculate(updatedResults)
                    checkNotifications(result, updatedStats)
                    state.copy(results = updatedResults, stats = updatedStats)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
    }
}
