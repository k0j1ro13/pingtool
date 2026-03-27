package com.pingmonitor.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingmonitor.domain.SpeedTestPhase
import com.pingmonitor.viewmodel.SpeedTestViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpeedTestScreen(viewModel: SpeedTestViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        val currentMbps = when (state.progress.phase) {
            SpeedTestPhase.DOWNLOAD -> state.progress.downloadMbps
            SpeedTestPhase.UPLOAD   -> state.progress.uploadMbps
            else                    -> null
        }

        SpeedGauge(
            currentMbps = currentMbps,
            phase       = state.progress.phase,
            isTesting   = state.isTesting
        )

        // Tarjetas de resultado con gradiente por métrica
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultCard(
                label     = "Ping",
                value     = state.progress.pingMs?.let { "%.0f ms".format(it) } ?: "—",
                emoji     = "🔁",
                accentColor = Color(0xFF42A5F5),
                modifier  = Modifier.weight(1f)
            )
            ResultCard(
                label     = "Descarga",
                value     = state.progress.downloadMbps?.let { "%.1f Mbps".format(it) } ?: "—",
                emoji     = "📥",
                accentColor = Color(0xFF66BB6A),
                modifier  = Modifier.weight(1f)
            )
            ResultCard(
                label     = "Subida",
                value     = state.progress.uploadMbps?.let { "%.1f Mbps".format(it) } ?: "—",
                emoji     = "📤",
                accentColor = Color(0xFFFF9800),
                modifier  = Modifier.weight(1f)
            )
        }

        val errorMsg = state.progress.error
        if (errorMsg != null) {
            Text(
                text      = errorMsg,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        if (state.isTesting) {
            OutlinedButton(
                onClick = viewModel::cancelTest,
                colors  = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cancelar")
            }
        } else {
            Button(onClick = viewModel::startTest) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = if (state.progress.phase == SpeedTestPhase.IDLE) "Iniciar test" else "Repetir test",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.progress.phase == SpeedTestPhase.IDLE) {
            Text(
                text      = "Usa los servidores de Cloudflare.\nCierra otras apps que consuman red para mayor precisión.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SpeedGauge(
    currentMbps: Double?,
    phase: SpeedTestPhase,
    isTesting: Boolean,
    modifier: Modifier = Modifier
) {
    val maxMbps    = 500.0
    val progress   = ((currentMbps ?: 0.0) / maxMbps).coerceIn(0.0, 1.0).toFloat()
    val speedColor = speedColor(currentMbps)

    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = tween(400),
        label        = "gauge"
    )

    val outlineColor  = MaterialTheme.colorScheme.outline
    val surfaceColor  = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth  = 22.dp.toPx()
            val glowWidth    = 36.dp.toPx()
            val padding      = strokeWidth / 2f + 2.dp.toPx()
            val arcSize      = Size(size.width - padding * 2, size.height - padding * 2)
            val topLeft      = Offset(padding, padding)
            val startAngle   = 150f
            val sweepTotal   = 240f

            // Fondo del gauge
            drawArc(
                color      = outlineColor.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Halo/glow del arco de progreso
            if (animatedProgress > 0f) {
                drawArc(
                    color      = speedColor.copy(alpha = 0.18f),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * animatedProgress,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = glowWidth, cap = StrokeCap.Round)
                )
                // Arco principal
                drawArc(
                    color      = speedColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * animatedProgress,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = if (currentMbps != null) "%.1f".format(currentMbps)
                             else if (phase == SpeedTestPhase.PING && isTesting) "…" else "—",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color      = if (currentMbps != null) speedColor
                             else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = "Mbps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (phase) {
                    SpeedTestPhase.PING     -> if (isTesting) "Midiendo ping…" else ""
                    SpeedTestPhase.DOWNLOAD -> "⬇  Descarga"
                    SpeedTestPhase.UPLOAD   -> "⬆  Subida"
                    SpeedTestPhase.DONE     -> "✓  Completado"
                    SpeedTestPhase.ERROR    -> "⚠  Error"
                    SpeedTestPhase.IDLE     -> "Listo"
                },
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ResultCard(
    label: String,
    value: String,
    emoji: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surfaceVariant
        )
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color      = if (value == "—") MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
            textAlign  = TextAlign.Center
        )
    }
}

private fun speedColor(mbps: Double?): Color = when {
    mbps == null || mbps < 10 -> Color(0xFFEF5350)
    mbps < 50                 -> Color(0xFFFF9800)
    mbps < 150                -> Color(0xFFFFEB3B)
    else                      -> Color(0xFF4CAF50)
}
