package com.pingmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pingmonitor.domain.PingResult
import com.pingmonitor.domain.PingStatus

@Composable
fun PingRow(result: PingResult) {
    val statusColor = when (result.status) {
        PingStatus.OK -> Color(0xFF2E7D32)
        PingStatus.TIMEOUT -> Color(0xFFE65100)
        PingStatus.ERROR -> Color(0xFFB71C1C)
    }
    val statusLabel = when (result.status) {
        PingStatus.OK -> "OK"
        PingStatus.TIMEOUT -> "TIMEOUT"
        PingStatus.ERROR -> "ERROR"
    }
    val rttText = result.rttMs?.let { "%.1f ms".format(it) } ?: "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barra de color lateral según el estado
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(42.dp)
                .background(statusColor)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número de secuencia
            Text(
                text = "#${result.seq}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp)
            )
            // Tamaño del paquete
            Text(
                text = "${result.sizeBytes} B",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(52.dp)
            )
            // RTT — resaltado si es OK
            Text(
                text = rttText,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (result.status == PingStatus.OK) FontWeight.SemiBold else FontWeight.Normal,
                color = if (result.status == PingStatus.OK) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
            )
            // Chip de estado con fondo de color tenue
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
