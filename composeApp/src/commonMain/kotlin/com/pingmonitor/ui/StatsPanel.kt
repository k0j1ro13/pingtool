package com.pingmonitor.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val animScore  by animateFloatAsState(stats.qualityScore.toFloat(), animSpec, label = "score")

    val rttColor = when {
        stats.received == 0        -> Color.Unspecified
        stats.rttAvg < RTT_GOOD   -> Color(0xFF2E7D32)
        stats.rttAvg < RTT_MEDIUM -> Color(0xFFF9A825)
        stats.rttAvg < RTT_HIGH   -> Color(0xFFE65100)
        else                       -> Color(0xFFB71C1C)
    }
    val lostColor = when {
        stats.lostPercent == 0.0 -> Color(0xFF2E7D32)
        stats.lostPercent < 5.0  -> Color(0xFFF9A825)
        else                     -> Color(0xFFB71C1C)
    }
    val scoreColor = when {
        stats.qualityScore >= 80 -> Color(0xFF2E7D32)
        stats.qualityScore >= 55 -> Color(0xFFF9A825)
        stats.qualityScore >= 30 -> Color(0xFFE65100)
        else                     -> Color(0xFFB71C1C)
    }
    val scoreLabel = when {
        stats.qualityScore >= 80 -> "Excelente"
        stats.qualityScore >= 55 -> "Buena"
        stats.qualityScore >= 30 -> "Aceptable"
        else                     -> "Mala"
    }

    val panelGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.surfaceVariant
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(panelGradient)
    ) {
        // ── Cabecera ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Text(
                text       = "📊  Estadísticas de sesión",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // ── Score + stats ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador circular de calidad
            if (stats.sent > 0) {
                QualityGauge(
                    score      = animScore,
                    scoreColor = scoreColor,
                    label      = scoreLabel
                )
            }

            // Columna derecha con stats
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Fila 1: enviados / perdidos / jitter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val jitterColor = when {
                        stats.received < 2 -> Color.Unspecified
                        stats.jitter < 5   -> Color(0xFF2E7D32)
                        stats.jitter < 20  -> Color(0xFFF9A825)
                        else               -> Color(0xFFB71C1C)
                    }
                    StatCell("Enviados", "${stats.sent}")
                    StatCell("Perdidos", "%.1f%%".format(animLost), lostColor)
                    StatCell("Jitter",   "%.1f ms".format(animJitter), jitterColor)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Fila 2: RTT mín / med / máx
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCell("RTT mín", "%.1f ms".format(animMin))
                    StatCell("RTT med", "%.1f ms".format(animAvg), rttColor)
                    StatCell("RTT máx", "%.1f ms".format(animMax))
                }
            }
        }
    }
}

@Composable
private fun QualityGauge(score: Float, scoreColor: Color, label: String) {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 7.dp.toPx()
            val pad    = stroke / 2f
            val arc    = Size(size.width - pad * 2, size.height - pad * 2)
            val tl     = Offset(pad, pad)

            // Track
            drawArc(
                color      = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter  = false,
                topLeft    = tl, size = arc,
                style      = Stroke(stroke, cap = StrokeCap.Round)
            )
            // Halo
            if (score > 0f) {
                drawArc(
                    color      = scoreColor.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f * (score / 100f),
                    useCenter  = false,
                    topLeft    = tl, size = arc,
                    style      = Stroke(stroke * 2f, cap = StrokeCap.Round)
                )
                // Arco principal
                drawArc(
                    color      = scoreColor,
                    startAngle = 135f,
                    sweepAngle = 270f * (score / 100f),
                    useCenter  = false,
                    topLeft    = tl, size = arc,
                    style      = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "${score.toInt()}",
                style      = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color      = scoreColor
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        8f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                ),
                color = scoreColor
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color      = valueColor
        )
    }
}
