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
    val error: String? = null,
    val hostnames: Map<String, String> = emptyMap()
)

class NetworkInfoViewModel(private val repository: NetworkInfoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkInfoUiState())
    val uiState: StateFlow<NetworkInfoUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null, hostnames = emptyMap()) }
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
                if (info != null) resolveHostnames(info)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    private suspend fun resolveHostnames(info: NetworkInfo) {
        val ips = buildList {
            info.gateway?.let { add(it) }
            addAll(info.dnsServers)
            info.publicIp?.takeIf { !it.contains("…") && !it.contains("Obteniendo") }?.let { add(it) }
        }.distinct()

        val resolved = mutableMapOf<String, String>()
        for (ip in ips) {
            val name = repository.resolveHostname(ip)
            if (name != null) resolved[ip] = name
        }
        if (resolved.isNotEmpty()) {
            _uiState.update { it.copy(hostnames = resolved) }
        }
    }
}
