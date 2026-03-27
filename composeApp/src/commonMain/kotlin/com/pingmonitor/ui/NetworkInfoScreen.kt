package com.pingmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingmonitor.domain.ConnectionType
import com.pingmonitor.domain.NetworkInfo
import com.pingmonitor.viewmodel.NetworkInfoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NetworkInfoScreen(viewModel: NetworkInfoViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón actualizar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.isLoading) "Obteniendo información…" else "Información de tu red",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = viewModel::refresh,
                enabled = !state.isLoading
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Actualizar")
            }
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Recopilando datos de red…\n(puede tardar unos segundos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.displaySmall)
                        Text(
                            text = state.error ?: "Error desconocido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            state.info != null -> {
                NetworkInfoContent(info = state.info!!, hostnames = state.hostnames)
            }
        }
    }
}

@Composable
private fun NetworkInfoContent(info: NetworkInfo, hostnames: Map<String, String> = emptyMap()) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Conexión ─────────────────────────────────────────────────────────
        InfoCard(title = "🔌  Conexión") {
            InfoRow("Interfaz",  info.interfaceName)
            InfoRow("Tipo", when (info.connectionType) {
                ConnectionType.WIFI     -> "Wi-Fi"
                ConnectionType.ETHERNET -> "Ethernet"
                ConnectionType.LOOPBACK -> "Loopback"
                ConnectionType.UNKNOWN  -> "Desconocido"
            })
        }

        // ── Señal WiFi (solo si es WiFi) ──────────────────────────────────
        if (info.connectionType == ConnectionType.WIFI) {
            InfoCard(title = "📶  Señal Wi-Fi") {
                if (info.ssid != null) InfoRow("Red (SSID)", info.ssid)

                if (info.signalPercent != null) {
                    val signalColor = when {
                        info.signalPercent >= 75 -> Color(0xFF2E7D32)
                        info.signalPercent >= 50 -> Color(0xFFF9A825)
                        info.signalPercent >= 25 -> Color(0xFFE65100)
                        else                     -> Color(0xFFB71C1C)
                    }
                    val quality = when {
                        info.signalPercent >= 75 -> "Excelente"
                        info.signalPercent >= 50 -> "Buena"
                        info.signalPercent >= 25 -> "Débil"
                        else                     -> "Muy débil"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Intensidad",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(100.dp)
                        )
                        LinearProgressIndicator(
                            progress = { info.signalPercent / 100f },
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)),
                            color = signalColor,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "${info.signalPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = signalColor,
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge de calidad
                        Box(
                            modifier = Modifier
                                .background(signalColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = quality,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = signalColor
                            )
                        }
                        if (info.signalDbm != null) {
                            Text(
                                text = "${info.signalDbm} dBm",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    InfoRow("Intensidad", "No disponible")
                }
            }
        }

        // ── Direccionamiento ─────────────────────────────────────────────
        InfoCard(title = "🌐  Direccionamiento IP") {
            InfoRow("IP local",         info.localIp     ?: "—")
            InfoRow("Máscara",          info.subnetMask  ?: "—")
            val gw = info.gateway ?: "—"
            InfoRow("Puerta de enlace", gw, hostnames[gw])
            if (info.ipv6 != null) {
                InfoRow("IPv6", info.ipv6)
            }
            InfoRow("IP pública",       info.publicIp    ?: "Obteniendo…", info.publicIp?.let { hostnames[it] })
        }

        // ── DNS ───────────────────────────────────────────────────────────
        InfoCard(title = "🔎  Servidores DNS") {
            if (info.dnsServers.isEmpty()) {
                InfoRow("DNS", "No detectados")
            } else {
                info.dnsServers.forEachIndexed { i, dns ->
                    InfoRow("DNS ${i + 1}", dns, hostnames[dns])
                }
            }
        }

        // ── Identificación ───────────────────────────────────────────────
        InfoCard(title = "🔧  Identificación") {
            InfoRow("Dirección MAC", info.macAddress ?: "—")
            InfoRow("MTU",          info.mtu?.toString()?.plus(" bytes") ?: "—")
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Tarjeta con título y contenido. */
@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Cabecera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        // Contenido
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
        }
    }
}

/** Fila de etiqueta + valor (+ hostname opcional debajo). */
@Composable
private fun InfoRow(label: String, value: String, hostname: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Column(modifier = Modifier.weight(0.55f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hostname != null) {
                Text(
                    text = hostname,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}
