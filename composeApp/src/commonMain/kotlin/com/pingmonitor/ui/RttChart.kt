package com.pingmonitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pingmonitor.domain.PingResult

// Número máximo de puntos visibles en la gráfica
private const val CHART_MAX_POINTS = 50

// Umbrales de color para los puntos según la latencia
private val COLOR_OK      = Color(0xFF2E7D32)  // verde — rápido
private val COLOR_WARN    = Color(0xFFF9A825)  // amarillo — medio
private val COLOR_SLOW    = Color(0xFFB71C1C)  // rojo — lento

/**
 * Gráfica de RTT en tiempo real dibujada con Canvas.
 * Muestra etiquetas de mín/med/máx y colorea los puntos según la latencia.
 */
@Composable
fun RttChart(results: List<PingResult>, modifier: Modifier = Modifier) {
    val validPoints = results.filter { it.rttMs != null }.takeLast(CHART_MAX_POINTS)
    if (validPoints.size < 2) return

    val rttValues = validPoints.mapNotNull { it.rttMs }
    val minRtt = rttValues.min()
    val avgRtt = rttValues.average()
    val maxRtt = rttValues.max()

    val primaryColor      = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Etiquetas mín / med / máx
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "↓ ${"%.0f".format(minRtt)} ms",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = COLOR_OK,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ø ${"%.0f".format(avgRtt)} ms",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "↑ ${"%.0f".format(maxRtt)} ms",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (maxRtt > 300) COLOR_SLOW else if (maxRtt > 100) COLOR_WARN else COLOR_OK,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(surfaceVariantColor, RoundedCornerShape(8.dp))
        ) {
            val range = (maxRtt - minRtt).coerceAtLeast(10.0)

            val xPad = 12.dp.toPx()
            val yPad = 8.dp.toPx()
            val chartWidth  = size.width - 2 * xPad
            val chartHeight = size.height - 2 * yPad
            val xStep = chartWidth / (validPoints.size - 1).coerceAtLeast(1)

            fun yFor(rtt: Double): Float =
                (yPad + (1.0 - (rtt - minRtt) / range) * chartHeight).toFloat()

            // Línea de referencia horizontal (RTT promedio)
            val yAvg = yFor(avgRtt)
            drawLine(
                color = gridColor,
                start = Offset(xPad, yAvg),
                end   = Offset(size.width - xPad, yAvg),
                strokeWidth = 1.dp.toPx()
            )

            // Área rellena bajo la curva
            val fillPath = Path()
            fillPath.moveTo(xPad, size.height - yPad)
            validPoints.forEachIndexed { index, result ->
                val x = xPad + index * xStep
                val y = yFor(result.rttMs!!)
                fillPath.lineTo(x, y)
            }
            val lastX = xPad + (validPoints.size - 1) * xStep
            fillPath.lineTo(lastX, size.height - yPad)
            fillPath.close()
            drawPath(fillPath, color = primaryColor.copy(alpha = 0.08f))

            // Línea principal de RTT
            val linePath = Path()
            validPoints.forEachIndexed { index, result ->
                val x = xPad + index * xStep
                val y = yFor(result.rttMs!!)
                if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(
                path  = linePath,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Puntos individuales coloreados según la latencia
            validPoints.forEachIndexed { index, result ->
                val x = xPad + index * xStep
                val y = yFor(result.rttMs!!)
                val dotColor = when {
                    result.rttMs > 300 -> COLOR_SLOW
                    result.rttMs > 100 -> COLOR_WARN
                    else               -> COLOR_OK
                }
                drawCircle(color = dotColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
