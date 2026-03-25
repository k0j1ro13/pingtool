package com.pingmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pingmonitor.domain.PingSession
import com.pingmonitor.viewmodel.PingViewModel

@Composable
fun HistoryScreen(viewModel: PingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history = state.sessionHistory.reversed()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cabecera con botón limpiar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${history.size} sesión${if (history.size != 1) "es" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = viewModel::clearHistory) {
                    Text("Limpiar todo", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (history.isEmpty()) {
            // Empty state
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📋", style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = "Sin sesiones aún",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Las sesiones se guardan automáticamente\ncuando pulsas Detener",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: PingSession) {
    val lostColor = when {
        session.stats.lostPercent == 0.0 -> Color(0xFF2E7D32)
        session.stats.lostPercent < 5.0 -> Color(0xFFF9A825)
        else -> Color(0xFFB71C1C)
    }
    val qualityLabel = when {
        session.stats.lostPercent == 0.0 && session.stats.rttAvg < 50 -> "Excelente"
        session.stats.lostPercent < 2.0 && session.stats.rttAvg < 100 -> "Buena"
        session.stats.lostPercent < 10.0 -> "Aceptable"
        else -> "Mala"
    }
    val qualityColor = when (qualityLabel) {
        "Excelente" -> Color(0xFF2E7D32)
        "Buena" -> Color(0xFF00897B)
        "Aceptable" -> Color(0xFFF9A825)
        else -> Color(0xFFB71C1C)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Cabecera de la tarjeta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.host,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(session.startMs) + "  ·  " + formatDuration(session.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Badge de calidad
            Box(
                modifier = Modifier
                    .background(qualityColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = qualityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = qualityColor
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Estadísticas en grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HistStat("Enviados",  session.stats.sent.toString())
            HistStat("Recibidos", session.stats.received.toString())
            HistStat("Perdidos",  "%.1f%%".format(session.stats.lostPercent), lostColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HistStat("RTT mín", "%.1f ms".format(session.stats.rttMin))
            HistStat("RTT med", "%.1f ms".format(session.stats.rttAvg))
            HistStat("RTT máx", "%.1f ms".format(session.stats.rttMax))
        }
    }
}

@Composable
private fun HistStat(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

private fun formatTimestamp(ms: Long): String {
    val agoSec = (System.currentTimeMillis() - ms) / 1000
    return when {
        agoSec < 60 -> "Hace ${agoSec}s"
        agoSec < 3600 -> "Hace ${agoSec / 60}m"
        agoSec < 86400 -> "Hace ${agoSec / 3600}h ${(agoSec % 3600) / 60}m"
        else -> "Hace ${agoSec / 86400}d"
    }
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}
