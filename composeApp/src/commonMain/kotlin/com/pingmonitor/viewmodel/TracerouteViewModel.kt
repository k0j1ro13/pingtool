package com.pingmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingmonitor.data.TracerouteRepository
import com.pingmonitor.domain.TracerouteHop
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TracerouteUiState(
    val host: String = "",
    val hops: List<TracerouteHop> = emptyList(),
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)

class TracerouteViewModel(
    private val repository: TracerouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TracerouteUiState())
    val uiState: StateFlow<TracerouteUiState> = _uiState.asStateFlow()

    private var traceJob: Job? = null

    fun onHostChange(host: String) {
        _uiState.update { it.copy(host = host, errorMessage = null) }
    }

    fun startTrace() {
        val host = _uiState.value.host.trim()
        if (host.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Introduce una IP o hostname válido") }
            return
        }
        traceJob?.cancel()
        _uiState.update { it.copy(hops = emptyList(), isRunning = true, isFinished = false, errorMessage = null) }

        traceJob = viewModelScope.launch {
            repository.trace(host).collect { hop ->
                _uiState.update { it.copy(hops = it.hops + hop) }
            }
            _uiState.update { it.copy(isRunning = false, isFinished = true) }
        }
    }

    fun stopTrace() {
        traceJob?.cancel()
        traceJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        traceJob?.cancel()
    }
}
