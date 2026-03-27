package com.pingmonitor.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.pingmonitor.domain.PingStats

private const val RTT_GOOD   = 50.0
private const val RTT_MEDIUM = 100.0
private const val RTT_HIGH   = 200.0

@Composable
fun StatsPanel(stats: PingStats, modifier: Modifier = Modifier) {
    val animSpec   = spring<Float>(stiffness = Spring.StiffnessMediumLow)
    val animLost   by animateFloatAsState(stats.lostPercent.toFloat(), animSpec, label = "lost")
    val animAvg    by animateFloatAsState(stats.rttAvg.toFloat(),      animSpec, label = "avg")
    val animMin    by animateFloatAsState(stats.rttMin.toFloat(),       animSpec, label = "min")
    val animMax    by animateFloatAsState(stats.rttMax.toFloat(),       animSpec, label = "max")
    val animJitter by animateFloatAsState(stats.jitter.toFloat(),       animSpec, label = "jitter")

    val rttColor = when {
        stats.received == 0 -> Color.Unspecified
        stats.rttAvg < RTT_GOOD -> Color(0xFF2E7D32)
        stats.rttAvg < RTT_MEDIUM -> Color(0xFFF9A825)
        stats.rttAvg < RTT_HIGH -> Color(0xFFE65100)
        else -> Color(0xFFB71C1C)
    }
    val lostColor = when {
        stats.lostPercent == 0.0 -> Color(0xFF2E7D32)
        stats.lostPercent < 5.0 -> Color(0xFFF9A825)
        else -> Color(0xFFB71C1C)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Título
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Estadísticas de sesión",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Fila 1 — pérdida + jitter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val jitterColor = when {
                stats.received < 2 -> Color.Unspecified
                stats.jitter < 5   -> Color(0xFF2E7D32)
                stats.jitter < 20  -> Color(0xFFF9A825)
                else               -> Color(0xFFB71C1C)
            }
            StatCell("Enviados",  "${stats.sent}")
            StatCell("Perdidos",  "%.1f%%".format(animLost), lostColor)
            StatCell("Jitter",    "%.1f ms".format(animJitter), jitterColor)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Fila 2 — RTT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCell("RTT mín", "%.1f ms".format(animMin))
            StatCell("RTT med", "%.1f ms".format(animAvg), rttColor)
            StatCell("RTT máx", "%.1f ms".format(animMax))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
