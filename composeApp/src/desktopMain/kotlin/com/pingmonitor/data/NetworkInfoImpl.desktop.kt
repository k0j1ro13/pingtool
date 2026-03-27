package com.pingmonitor.data

import com.pingmonitor.domain.ConnectionType
import com.pingmonitor.domain.NetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.URL

actual class NetworkInfoImpl actual constructor() : NetworkInfoRepository {

    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    override suspend fun getNetworkInfo(): NetworkInfo? = withContext(Dispatchers.IO) {
        val (iface, ifAddr) = getBestInterface() ?: return@withContext null

        val localIp    = ifAddr.address.hostAddress
        val subnetMask = prefixToSubnet(ifAddr.networkPrefixLength)
        val macAddress = iface.hardwareAddress?.joinToString(":") { "%02X".format(it) }
        val mtu        = iface.mtu.takeIf { it > 0 }
        val ipv6       = iface.interfaceAddresses
            .firstOrNull { it.address is Inet6Address && !it.address.isLoopbackAddress }
            ?.address?.hostAddress

        val displayName = iface.displayName ?: iface.name
        val connectionType = detectConnectionType(iface)

        // Gateway y DNS del sistema
        val (gateway, dnsServers) = if (isWindows) ipconfigInfo() else unixNetworkInfo()

        // Info WiFi (solo Windows por ahora)
        val (ssid, signalPercent, signalDbm) =
            if (isWindows && connectionType == ConnectionType.WIFI) wifiInfoWindows()
            else Triple(null, null, null)

        // IP pública (con timeout corto para no bloquear)
        val publicIp = fetchPublicIp()

        NetworkInfo(
            interfaceName  = displayName,
            connectionType = connectionType,
            localIp        = localIp,
            subnetMask     = subnetMask,
            gateway        = gateway,
            ipv6           = ipv6,
            publicIp       = publicIp,
            dnsServers     = dnsServers,
            macAddress     = macAddress,
            mtu            = mtu,
            ssid           = ssid,
            signalPercent  = signalPercent,
            signalDbm      = signalDbm
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun getBestInterface(): Pair<NetworkInterface, java.net.InterfaceAddress>? =
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.filter { !it.isLoopback && it.isUp }
            ?.flatMap { ni ->
                ni.interfaceAddresses
                    .filter { it.address is Inet4Address }
                    .map { ni to it }
            }
            ?.firstOrNull()

    private fun prefixToSubnet(prefix: Short): String {
        val mask = if (prefix <= 0) 0L else ((-1L) shl (32 - prefix)) and 0xFFFFFFFFL
        return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
    }

    private fun detectConnectionType(iface: NetworkInterface): ConnectionType {
        val name = (iface.displayName ?: iface.name).lowercase()
        return when {
            iface.isLoopback -> ConnectionType.LOOPBACK
            name.contains("wi-fi") || name.contains("wireless") ||
            name.contains("wlan") || name.startsWith("wl") -> ConnectionType.WIFI
            name.contains("ethernet") || name.contains("eth") ||
            name.startsWith("en") -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }

    /** Extrae gateway y DNS ejecutando `ipconfig /all` en Windows. */
    private fun ipconfigInfo(): Pair<String?, List<String>> = try {
        val out = runCommand("ipconfig", "/all")

        val gateway = Regex(
            """(?:Default Gateway|Puerta de enlace predeterminada)[^:]*:\s*([\d.]+)""",
            RegexOption.IGNORE_CASE
        ).find(out)?.groupValues?.get(1)?.trim()

        val dns = Regex(
            """(?:DNS Servers?|Servidores DNS)[^:]*:\s*([\d.]+)""",
            RegexOption.IGNORE_CASE
        ).findAll(out).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()

        gateway to dns
    } catch (_: Exception) { null to emptyList() }

    /** Extrae gateway y DNS en Linux/macOS. */
    private fun unixNetworkInfo(): Pair<String?, List<String>> = try {
        val routeOut = runCommand("ip", "route", "show", "default")
        val gateway  = Regex("default via ([\\d.]+)").find(routeOut)?.groupValues?.get(1)

        val dns = try {
            java.io.File("/etc/resolv.conf").readLines()
                .filter { it.startsWith("nameserver") }
                .map { it.substringAfter("nameserver").trim() }
        } catch (_: Exception) { emptyList() }

        gateway to dns
    } catch (_: Exception) { null to emptyList() }

    /** Extrae SSID y señal de `netsh wlan show interfaces` en Windows. */
    private fun wifiInfoWindows(): Triple<String?, Int?, Int?> = try {
        val out = runCommand("netsh", "wlan", "show", "interfaces")

        val ssid = Regex("""^\s+SSID\s*:\s*(.+)$""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
            .find(out)?.groupValues?.get(1)?.trim()

        val signalPct = Regex("""Signal\s*:\s*(\d+)%""", RegexOption.IGNORE_CASE)
            .find(out)?.groupValues?.get(1)?.toIntOrNull()

        // Aproximación de dBm a partir del porcentaje (fórmula estándar)
        val signalDbm = signalPct?.let { pct -> (pct.toDouble() / 2.0 - 100).toInt() }

        Triple(ssid, signalPct, signalDbm)
    } catch (_: Exception) { Triple(null, null, null) }

    /** Consulta la IP pública a través de un servicio externo. */
    private fun fetchPublicIp(): String? = try {
        val conn = URL("https://api.ipify.org").openConnection() as HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout    = 4000
        val ip = conn.inputStream.bufferedReader().readText().trim()
        conn.disconnect()
        ip.takeIf { it.matches(Regex("""[\d.]+""")) }
    } catch (_: Exception) { null }

    override suspend fun resolveHostname(ip: String): String? = withContext(Dispatchers.IO) {
        try {
            val name = java.net.InetAddress.getByName(ip).canonicalHostName
            if (name == ip) null else name
        } catch (_: Exception) { null }
    }

    private fun runCommand(vararg args: String): String {
        val process = ProcessBuilder(*args).redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return out
    }
}
