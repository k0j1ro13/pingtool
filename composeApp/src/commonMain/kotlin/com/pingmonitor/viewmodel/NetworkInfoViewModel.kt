package com.pingmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingmonitor.data.NetworkInfoRepository
import com.pingmonitor.domain.NetworkInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetworkInfoUiState(
    val info: NetworkInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NetworkInfoViewModel(private val repository: NetworkInfoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkInfoUiState())
    val uiState: StateFlow<NetworkInfoUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val info = repository.getNetworkInfo()
                _uiState.update {
                    it.copy(
                        info = info,
                        isLoading = false,
                        error = if (info == null) "No se pudo obtener información de red" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
}
