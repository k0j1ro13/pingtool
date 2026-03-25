package com.pingmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingmonitor.domain.NetworkDevice
import com.pingmonitor.domain.NetworkScanUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetworkScanUiState(
    val subnet: String = "",
    val devices: List<NetworkDevice> = emptyList(),
    val isScanning: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 254,
    val errorMessage: String? = null
)

class NetworkScanViewModel(private val scanUseCase: NetworkScanUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkScanUiState())
    val uiState: StateFlow<NetworkScanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Detectar subred local al iniciar
        val subnet = scanUseCase.getLocalSubnet()
        if (subnet != null) {
            _uiState.update { it.copy(subnet = subnet) }
        }
    }

    fun onSubnetChange(subnet: String) {
        _uiState.update { it.copy(subnet = subnet, errorMessage = null) }
    }

    fun startScan() {
        val subnet = _uiState.value.subnet.trim()
        if (subnet.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No se pudo detectar la subred. Introdúcela manualmente.") }
            return
        }

        scanJob?.cancel()
        _uiState.update {
            it.copy(devices = emptyList(), isScanning = true, scanned = 0, errorMessage = null)
        }

        scanJob = viewModelScope.launch {
            scanUseCase.scan(subnet).collect { device ->
                _uiState.update { state ->
                    val updatedDevices = if (device.isReachable) {
                        (state.devices + device).sortedBy {
                            it.ip.substringAfterLast(".").toIntOrNull() ?: 0
                        }
                    } else {
                        state.devices
                    }
                    state.copy(
                        devices = updatedDevices,
                        scanned = state.scanned + 1
                    )
                }
            }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
