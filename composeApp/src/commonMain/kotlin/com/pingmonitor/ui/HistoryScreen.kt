package com.pingmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = "${history.size} sesión${if (history.size != 1) "es" else ""}",
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📋", style = MaterialTheme.typography.displayMedium)
                    Text(
                        text       = "Sin sesiones aún",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text  = "Las sesiones se guardan automáticamente\ncuando pulsas Detener",
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
        session.stats.lostPercent == 0.0  -> Color(0xFF2E7D32)
        session.stats.lostPercent < 5.0   -> Color(0xFFF9A825)
        else                              -> Color(0xFFB71C1C)
    }
    val qualityLabel = when {
        session.stats.lostPercent == 0.0 && session.stats.rttAvg < 50  -> "Excelente"
        session.stats.lostPercent < 2.0  && session.stats.rttAvg < 100 -> "Buena"
        session.stats.lostPercent < 10.0                                -> "Aceptable"
        else                                                            -> "Mala"
    }
    val qualityColor = when (qualityLabel) {
        "Excelente" -> Color(0xFF2E7D32)
        "Buena"     -> Color(0xFF00897B)
        "Aceptable" -> Color(0xFFF9A825)
        else        -> Color(0xFFB71C1C)
    }

    val cardGradient = Brush.horizontalGradient(
        colors = listOf(qualityColor.copy(alpha = 0.10f), MaterialTheme.colorScheme.surfaceVariant)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Barra lateral de calidad
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(130.dp)
                .background(qualityColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .background(cardGradient)
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = session.host,
                        style      = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = formatTimestamp(session.startMs) + "  ·  " + formatDuration(session.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(qualityColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = qualityLabel,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = qualityColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Barra de pérdida de paquetes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text  = "Pérdida",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
                LinearProgressIndicator(
                    progress = { (session.stats.lostPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).height(5.dp),
                    color = lostColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Text(
                    text       = "%.1f%%".format(session.stats.lostPercent),
                    style      = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color      = lostColor,
                    modifier   = Modifier.width(40.dp)
                )
            }

            // Estadísticas en grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistStat("Enviados",  session.stats.sent.toString())
                HistStat("Recibidos", session.stats.received.toString())
                HistStat("RTT med",   "%.1f ms".format(session.stats.rttAvg))
                HistStat("RTT máx",   "%.1f ms".format(session.stats.rttMax))
            }
            Box(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HistStat(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(74.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
    }
}

private fun formatTimestamp(ms: Long): String {
    val agoSec = (System.currentTimeMillis() - ms) / 1000
    return when {
        agoSec < 60    -> "Hace ${agoSec}s"
        agoSec < 3600  -> "Hace ${agoSec / 60}m"
        agoSec < 86400 -> "Hace ${agoSec / 3600}h ${(agoSec % 3600) / 60}m"
        else           -> "Hace ${agoSec / 86400}d"
    }
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60   -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else     -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}
