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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingmonitor.domain.PingResult

private const val CHART_MAX_POINTS = 60

private val COLOR_OK   = Color(0xFF2E7D32)
private val COLOR_WARN = Color(0xFFF9A825)
private val COLOR_SLOW = Color(0xFFB71C1C)

@Composable
fun RttChart(results: List<PingResult>, modifier: Modifier = Modifier) {
    val validPoints = results.filter { it.rttMs != null }.takeLast(CHART_MAX_POINTS)
    if (validPoints.size < 2) return

    val rttValues = validPoints.mapNotNull { it.rttMs }
    val minRtt = rttValues.min()
    val avgRtt = rttValues.average()
    val maxRtt = rttValues.max()

    val primaryColor        = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor           = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

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
                textAlign = TextAlign.Center
            )
            Text(
                text = "↑ ${"%.0f".format(maxRtt)} ms",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (maxRtt > 300) COLOR_SLOW else if (maxRtt > 100) COLOR_WARN else COLOR_OK,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
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
            val yPad = 10.dp.toPx()
            val chartWidth  = size.width - 2 * xPad
            val chartHeight = size.height - 2 * yPad
            val xStep = if (validPoints.size > 1) chartWidth / (validPoints.size - 1) else chartWidth

            fun yFor(rtt: Double): Float =
                (yPad + (1.0 - (rtt - minRtt) / range) * chartHeight).toFloat()

            // Línea de referencia (promedio)
            val yAvg = yFor(avgRtt)
            drawLine(
                color = gridColor,
                start = Offset(xPad, yAvg),
                end   = Offset(size.width - xPad, yAvg),
                strokeWidth = 1.dp.toPx()
            )

            // Calcular puntos
            val pts = validPoints.mapIndexed { i, r ->
                Offset(xPad + i * xStep, yFor(r.rttMs!!))
            }

            // Área rellena con gradiente vertical usando Bezier
            val fillPath = Path()
            fillPath.moveTo(pts.first().x, size.height - yPad)
            fillPath.lineTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val cp1x = (pts[i - 1].x + pts[i].x) / 2f
                fillPath.cubicTo(cp1x, pts[i - 1].y, cp1x, pts[i].y, pts[i].x, pts[i].y)
            }
            fillPath.lineTo(pts.last().x, size.height - yPad)
            fillPath.close()
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.22f), primaryColor.copy(alpha = 0.0f)),
                    startY = yPad,
                    endY   = size.height - yPad
                )
            )

            // Línea principal con curvas Bezier suaves
            val linePath = Path()
            linePath.moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val cp1x = (pts[i - 1].x + pts[i].x) / 2f
                linePath.cubicTo(cp1x, pts[i - 1].y, cp1x, pts[i].y, pts[i].x, pts[i].y)
            }
            drawPath(
                path  = linePath,
                color = primaryColor,
                style = Stroke(
                    width = 2.2.dp.toPx(),
                    cap   = StrokeCap.Round,
                    join  = StrokeJoin.Round
                )
            )

            // Puntos coloreados por latencia con halo
            pts.forEachIndexed { i, pt ->
                val rtt = validPoints[i].rttMs!!
                val dotColor = when {
                    rtt > 300 -> COLOR_SLOW
                    rtt > 100 -> COLOR_WARN
                    else      -> COLOR_OK
                }
                drawCircle(color = dotColor.copy(alpha = 0.18f), radius = 5.dp.toPx(), center = pt)
                drawCircle(color = dotColor, radius = 3.dp.toPx(), center = pt)
            }

            // Último punto destacado con halo más grande
            val last = pts.last()
            val lastRtt = validPoints.last().rttMs!!
            val lastColor = when {
                lastRtt > 300 -> COLOR_SLOW
                lastRtt > 100 -> COLOR_WARN
                else          -> COLOR_OK
            }
            drawCircle(color = lastColor.copy(alpha = 0.28f), radius = 7.5.dp.toPx(), center = last)
            drawCircle(color = lastColor, radius = 4.dp.toPx(), center = last)
        }
    }
}
