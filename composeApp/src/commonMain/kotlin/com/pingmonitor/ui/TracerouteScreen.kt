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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingmonitor.domain.HopStatus
import com.pingmonitor.domain.TracerouteHop
import com.pingmonitor.viewmodel.TracerouteViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TracerouteScreen(viewModel: TracerouteViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Campo destino
        OutlinedTextField(
            value         = state.host,
            onValueChange = viewModel::onHostChange,
            label         = { Text("IP o Hostname destino") },
            placeholder   = { Text("ej. 8.8.8.8 o google.com") },
            singleLine    = true,
            isError       = state.errorMessage != null,
            supportingText = {
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    ?: Text(
                        "Muestra todos los saltos hasta el destino",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            },
            enabled  = !state.isRunning,
            modifier = Modifier.fillMaxWidth()
        )

        // Botones
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick  = viewModel::startTrace,
                enabled  = !state.isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Iniciar traza")
            }
            OutlinedButton(
                onClick  = viewModel::stopTrace,
                enabled  = state.isRunning,
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Detener")
            }
        }

        // Barra de progreso indeterminada mientras traza
        if (state.isRunning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color    = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Estado vacío / completado / en curso
        when {
            state.hops.isEmpty() && !state.isRunning -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛤️", style = MaterialTheme.typography.displayMedium)
                        Text(
                            text       = "Traza de ruta",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text  = "Muestra cada salto (router) entre\nte y el destino con su latencia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            state.hops.size == 1 && state.hops.first().status == HopStatus.ERROR -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.displaySmall)
                        Text(
                            text  = "Traceroute no disponible en este dispositivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                // Resumen: destino + nº de saltos
                if (state.isFinished && state.hops.isNotEmpty()) {
                    val responded = state.hops.count { it.status == HopStatus.RESPONDED }
                    val avgRtt = state.hops.mapNotNull { it.rttMs }.average().let {
                        if (it.isNaN()) null else it
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "${state.hops.size} saltos · $responded respondieron",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (avgRtt != null) {
                            Text(
                                text       = "RTT medio: ${"%.1f".format(avgRtt)} ms",
                                style      = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.hops, key = { it.ttl }) { hop ->
                        HopRow(hop)
                    }
                }
            }
        }
    }
}

@Composable
private fun HopRow(hop: TracerouteHop) {
    val (dotColor, rttColor) = when (hop.status) {
        HopStatus.RESPONDED -> {
            val rtt = hop.rttMs ?: 0.0
            val c = when {
                rtt < 20  -> Color(0xFF2E7D32)
                rtt < 80  -> Color(0xFFF9A825)
                rtt < 200 -> Color(0xFFE65100)
                else      -> Color(0xFFB71C1C)
            }
            c to c
        }
        HopStatus.TIMEOUT -> Color(0xFF9E9E9E) to Color(0xFF9E9E9E)
        HopStatus.ERROR   -> Color(0xFFB71C1C) to Color(0xFFB71C1C)
    }

    val gradient = Brush.horizontalGradient(
        colors = listOf(dotColor.copy(alpha = 0.07f), MaterialTheme.colorScheme.surfaceVariant)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(gradient)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Número de salto en círculo
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.15f))
        ) {
            Text(
                text       = "${hop.ttl}",
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color      = dotColor
            )
        }

        // IP o asteriscos
        Text(
            text       = hop.ip ?: "* * *",
            style      = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color      = if (hop.ip != null) MaterialTheme.colorScheme.onSurface
                         else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier   = Modifier.weight(1f)
        )

        // RTT
        Text(
            text       = hop.rttMs?.let { "%.1f ms".format(it) } ?: "—",
            style      = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color      = rttColor
        )
    }
}
