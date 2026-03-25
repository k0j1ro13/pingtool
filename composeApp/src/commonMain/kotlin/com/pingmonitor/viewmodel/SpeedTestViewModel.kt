package com.pingmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingmonitor.domain.SpeedTestPhase
import com.pingmonitor.domain.SpeedTestProgress
import com.pingmonitor.domain.SpeedTestUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeedTestUiState(
    val progress: SpeedTestProgress = SpeedTestProgress(),
    val isTesting: Boolean = false
)

class SpeedTestViewModel(private val useCase: SpeedTestUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    private var testJob: Job? = null

    fun startTest() {
        testJob?.cancel()
        _uiState.update {
            it.copy(
                isTesting = true,
                progress = SpeedTestProgress(phase = SpeedTestPhase.PING)
            )
        }
        testJob = viewModelScope.launch {
            useCase.execute().collect { progress ->
                _uiState.update {
                    it.copy(
                        progress = progress,
                        isTesting = !progress.isDone && progress.phase != SpeedTestPhase.ERROR
                    )
                }
            }
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _uiState.update { it.copy(isTesting = false) }
    }

    override fun onCleared() {
        super.onCleared()
        testJob?.cancel()
    }
}
