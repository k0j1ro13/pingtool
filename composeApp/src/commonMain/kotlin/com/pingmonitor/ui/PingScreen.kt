package com.pingmonitor.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingmonitor.domain.PingResult
import com.pingmonitor.domain.PingStatus
import com.pingmonitor.viewmodel.PingViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private val INTERVAL_OPTIONS = listOf(
    500L to "0.5 s",
    1000L to "1 s",
    2000L to "2 s",
    5000L to "5 s"
)

private val SIZE_OPTIONS: List<Pair<Int?, String>> = listOf(
    null to "Automático",
    32 to "32 B",
    64 to "64 B",
    128 to "128 B",
    256 to "256 B",
    512 to "512 B",
    1024 to "1024 B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(viewModel: PingViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Temporizador de sesión
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(state.sessionStartMs) {
        val startMs = state.sessionStartMs
        if (startMs != null) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - startMs) / 1000
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    // Auto-scroll al último resultado
    LaunchedEffect(state.results.size) {
        if (state.results.isNotEmpty()) {
            listState.animateScrollToItem(state.results.size - 1)
        }
    }

    // Punto pulsante de actividad
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Campo de IP con indicador de actividad
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.host,
                onValueChange = viewModel::onHostChange,
                label = { Text("IP o Hostname") },
                placeholder = { Text("ej. 8.8.8.8") },
                singleLine = true,
                isError = state.errorMessage != null,
                supportingText = {
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    when {
                        state.isPaused -> viewModel.resumePing()
                        !state.isRunning -> viewModel.startPing()
                    }
                }),
                modifier = Modifier.weight(1f),
                enabled = !state.isRunning
            )
            if (state.isRunning) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .alpha(pulseAlpha)
                )
            }
        }

        // Selectores de intervalo y tamaño
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IntervalDropdown(
                selectedMs = state.intervalMs,
                onSelect = viewModel::onIntervalChange,
                enabled = !state.isRunning,
                modifier = Modifier.weight(1f)
            )
            SizeDropdown(
                selectedBytes = state.selectedSizeBytes,
                onSelect = viewModel::onSizeChange,
                enabled = !state.isRunning,
                modifier = Modifier.weight(1f)
            )
        }

        // Botones con iconos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (state.isPaused) viewModel.resumePing() else viewModel.startPing() },
                enabled = !state.isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (state.isPaused) "Reanudar" else "Iniciar")
            }
            OutlinedButton(
                onClick = viewModel::pausePing,
                enabled = state.isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text("⏸  Pausar")
            }
            OutlinedButton(
                onClick = viewModel::stopPing,
                enabled = state.isRunning || state.isPaused,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Detener")
            }
        }

        // Zona de resultados o empty state
        if (state.results.isEmpty() && !state.isRunning && !state.isPaused) {
            // Empty state
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📡", style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = "Listo para monitorizar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Introduce una IP o hostname y pulsa Iniciar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Hero: último RTT
            state.results.lastOrNull()?.let { last ->
                RttHeroCard(last)
            }

            // Gráfica de RTT
            if (state.results.any { it.rttMs != null }) {
                RttChart(results = state.results)
            }

            // Cabecera de tabla + tiempo + limpiar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Seq", "Tamaño", "RTT", "Estado").forEach {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (state.sessionStartMs != null) {
                    val h = elapsedSeconds / 3600
                    val m = (elapsedSeconds % 3600) / 60
                    val s = elapsedSeconds % 60
                    Text(
                        text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = viewModel::clearResults,
                    enabled = !state.isRunning
                ) {
                    Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Lista de resultados
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(state.results, key = { it.seq }) { result ->
                    PingRow(result)
                }
            }

            // Panel de estadísticas
            Spacer(Modifier.height(2.dp))
            StatsPanel(stats = state.stats)
        }
    }
}

/** Tarjeta hero con el último RTT recibido. */
@Composable
private fun RttHeroCard(last: PingResult) {
    val statusColor = when (last.status) {
        PingStatus.OK -> Color(0xFF2E7D32)
        PingStatus.TIMEOUT -> Color(0xFFE65100)
        PingStatus.ERROR -> Color(0xFFB71C1C)
    }
    val statusLabel = when (last.status) {
        PingStatus.OK -> "✓  Paquete recibido"
        PingStatus.TIMEOUT -> "⏱  Timeout"
        PingStatus.ERROR -> "✗  Error"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Último paquete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = last.rttMs?.let { "%.1f".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (last.rttMs != null) {
                    Text(
                        text = "ms",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
        // Badge de estado
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalDropdown(
    selectedMs: Long,
    onSelect: (Long) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = INTERVAL_OPTIONS.find { it.first == selectedMs }?.second ?: "1 s"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Intervalo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            enabled = enabled,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            INTERVAL_OPTIONS.forEach { (ms, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(ms); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeDropdown(
    selectedBytes: Int?,
    onSelect: (Int?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = SIZE_OPTIONS.find { it.first == selectedBytes }?.second ?: "Automático"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tamaño") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            enabled = enabled,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SIZE_OPTIONS.forEach { (bytes, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(bytes); expanded = false })
            }
        }
    }
}
