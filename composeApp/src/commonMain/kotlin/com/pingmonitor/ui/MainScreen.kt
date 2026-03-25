package com.pingmonitor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pingmonitor.viewmodel.PingViewModel
import org.koin.compose.viewmodel.koinViewModel

private data class TabItem(val label: String, val icon: ImageVector?, val emoji: String? = null)

private val TABS = listOf(
    TabItem("Ping",      Icons.Rounded.Refresh),
    TabItem("Red",       Icons.Rounded.Search),
    TabItem("Mi Red",    Icons.Rounded.Star),
    TabItem("Historial", Icons.Rounded.List),
    TabItem("Velocidad", icon = null, emoji = "⚡")
)

private val TAB_TITLES = listOf("PingTool", "Escáner de Red", "Mi Red", "Historial", "Test de Velocidad")

private data class HelpSection(val title: String, val body: String)

private val HELP_CONTENT = listOf(
    // Tab 0 — Ping
    listOf(
        HelpSection("📡  ¿Qué es el ping?",
            "El ping envía pequeños paquetes de datos a una IP o nombre de servidor (como 8.8.8.8 o google.com) y mide cuánto tardan en volver. Así sabes si hay conexión y qué calidad tiene."),
        HelpSection("▶  Iniciar / ⏸  Pausar / ⏹  Detener",
            "• Iniciar: comienza a enviar paquetes de forma continua.\n• Pausar: detiene el envío temporalmente sin borrar los resultados.\n• Detener: finaliza la sesión y la guarda en el Historial."),
        HelpSection("📊  Resultados",
            "• RTT (ms): tiempo de ida y vuelta del paquete. Cuanto menor, mejor.\n• OK: el servidor respondió.\n• TIMEOUT: el servidor no respondió a tiempo.\n• ERROR: no se pudo contactar con el servidor."),
        HelpSection("⚙️  Opciones",
            "• Intervalo: tiempo entre cada ping (0.5 s más rápido, 5 s más lento).\n• Tamaño: cuántos bytes lleva cada paquete. 'Automático' los rota para probar distintos tamaños.")
    ),
    // Tab 1 — Escáner de Red
    listOf(
        HelpSection("🔍  ¿Qué es el escáner de red?",
            "Detecta todos los dispositivos activos en tu red local (router, móviles, PCs, smart TVs…) comprobando si responden a un ping en el rango de IPs de tu subred."),
        HelpSection("🌐  Subred",
            "Es el prefijo de tu red local, por ejemplo '192.168.1'. La app lo detecta automáticamente. Si ves un error, puedes escribirlo manualmente.\n\nPuedes consultarlo en Configuración → Red → Ver detalles de la conexión."),
        HelpSection("⏱  Duración del escaneo",
            "El escaneo comprueba 254 direcciones en paralelo. Suele tardar entre 5 y 30 segundos según la velocidad de tu red."),
        HelpSection("💡  Consejo",
            "Algunos dispositivos (móviles con batería optimizada o firewalls activos) pueden no responder al ping aunque estén conectados a la red.")
    ),
    // Tab 2 — Mi Red
    listOf(
        HelpSection("📋  ¿Qué muestra esta pantalla?",
            "Recopila automáticamente toda la información de tu conexión de red actual: dirección IP, máscara de subred, puerta de enlace, servidores DNS, dirección MAC y más."),
        HelpSection("📶  Señal Wi-Fi",
            "Si estás conectado por Wi-Fi, verás el nombre de la red (SSID) y la intensidad de la señal en porcentaje y en dBm.\n\n• > 75%: Excelente (–50 a –65 dBm)\n• 50–75%: Buena (–65 a –75 dBm)\n• 25–50%: Débil (–75 a –85 dBm)\n• < 25%: Muy débil (< –85 dBm)"),
        HelpSection("🌐  IP pública vs IP local",
            "• IP local: la que te asigna tu router dentro de casa (ej. 192.168.1.5). Solo es accesible desde tu red.\n• IP pública: la que ve el mundo exterior. La comparten todos los dispositivos de tu red doméstica."),
        HelpSection("🔄  Actualizar",
            "Pulsa el botón 'Actualizar' para volver a recopilar la información. Útil si has cambiado de red Wi-Fi o si la IP pública ha cambiado.")
    ),
    // Tab 3 — Historial
    listOf(
        HelpSection("📋  ¿Qué es el historial?",
            "Aquí se guardan automáticamente todas las sesiones de ping que hayas finalizado con el botón Detener. Puedes revisar el rendimiento de conexiones anteriores."),
        HelpSection("🏷️  Calidad de sesión",
            "Cada sesión muestra una etiqueta de calidad:\n• Excelente: sin pérdidas y RTT < 50 ms.\n• Buena: pérdidas mínimas y RTT < 100 ms.\n• Aceptable: algunas pérdidas o latencia alta.\n• Mala: muchas pérdidas o latencia muy alta."),
        HelpSection("🗑️  Limpiar historial",
            "Pulsa 'Limpiar todo' para borrar todas las sesiones. El historial solo persiste mientras la app está abierta; se borra al cerrarla.")
    ),
    // Tab 4 — Velocidad
    listOf(
        HelpSection("⚡  ¿Qué mide el test?",
            "Mide tres aspectos de tu conexión:\n• Ping: tiempo de respuesta al servidor (ms).\n• Descarga: velocidad a la que recibes datos (Mbps).\n• Subida: velocidad a la que envías datos (Mbps)."),
        HelpSection("🌐  Servidor de prueba",
            "El test usa los servidores de Cloudflare, uno de los más rápidos y distribuidos del mundo. El resultado puede variar según la hora del día y la carga de tu red."),
        HelpSection("📊  Interpretar resultados",
            "• < 10 Mbps: conexión lenta.\n• 10–100 Mbps: conexión normal.\n• > 100 Mbps: conexión rápida.\n\nLa subida suele ser más lenta que la descarga en conexiones de fibra asimétricas."),
        HelpSection("💡  Consejo",
            "Para mayor precisión, cierra otras apps que consuman red y realiza el test varias veces en distintos momentos del día.")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showHelp by remember { mutableStateOf(false) }
    val pingViewModel: PingViewModel = koinViewModel()

    if (showHelp) {
        HelpDialog(tabIndex = selectedTab, onDismiss = { showHelp = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(TAB_TITLES[selectedTab], fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                TABS.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            if (tab.icon != null) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = tab.emoji ?: "",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> PingScreen(viewModel = pingViewModel)
                1 -> NetworkScanScreen()
                2 -> NetworkInfoScreen()
                3 -> HistoryScreen(viewModel = pingViewModel)
                4 -> SpeedTestScreen()
            }
        }
    }
}

@Composable
private fun HelpDialog(tabIndex: Int, onDismiss: () -> Unit) {
    val sections = HELP_CONTENT[tabIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        },
        title = { Row { Text("Guía — ${TAB_TITLES[tabIndex]}") } },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
