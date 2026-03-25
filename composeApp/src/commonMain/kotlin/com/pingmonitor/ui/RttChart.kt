package com.pingmonitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pingmonitor.domain.PingResult

// Número máximo de puntos visibles en la gráfica
private const val CHART_MAX_POINTS = 50

/**
 * Gráfica de RTT en tiempo real dibujada con Canvas.
 * Solo se renderiza cuando hay al menos 2 puntos con RTT válido.
 */
@Composable
fun RttChart(results: List<PingResult>, modifier: Modifier = Modifier) {
    val validPoints = results.filter { it.rttMs != null }.takeLast(CHART_MAX_POINTS)
    if (validPoints.size < 2) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(surfaceVariantColor, RoundedCornerShape(8.dp))
    ) {
        val maxRtt = validPoints.maxOf { it.rttMs!! }
        val minRtt = validPoints.minOf { it.rttMs!! }
        val range = (maxRtt - minRtt).coerceAtLeast(10.0)

        val xPad = 12.dp.toPx()
        val yPad = 8.dp.toPx()
        val chartWidth = size.width - 2 * xPad
        val chartHeight = size.height - 2 * yPad
        val xStep = chartWidth / (validPoints.size - 1).coerceAtLeast(1)

        fun yFor(rtt: Double): Float =
            (yPad + (1.0 - (rtt - minRtt) / range) * chartHeight).toFloat()

        // Línea de referencia horizontal (RTT promedio)
        val avgRtt = validPoints.mapNotNull { it.rttMs }.average()
        val yAvg = yFor(avgRtt)
        drawLine(
            color = gridColor,
            start = Offset(xPad, yAvg),
            end = Offset(size.width - xPad, yAvg),
            strokeWidth = 1.dp.toPx()
        )

        // Línea principal de RTT
        val path = Path()
        validPoints.forEachIndexed { index, result ->
            val x = xPad + index * xStep
            val y = yFor(result.rttMs!!)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Puntos individuales
        validPoints.forEachIndexed { index, result ->
            val x = xPad + index * xStep
            val y = yFor(result.rttMs!!)
            drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}
