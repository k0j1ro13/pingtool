package com.pingmonitor.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.pingmonitor.domain.ConnectionType
import com.pingmonitor.domain.NetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.URL

actual class NetworkInfoImpl actual constructor() : NetworkInfoRepository {

    companion object {
        var appContext: Context? = null
    }

    override suspend fun getNetworkInfo(): NetworkInfo? = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext null

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@withContext null

        val network = cm.activeNetwork ?: return@withContext null
        val caps = cm.getNetworkCapabilities(network) ?: return@withContext null
        val linkProps = cm.getLinkProperties(network)

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isEth = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val connectionType = when {
            isWifi -> ConnectionType.WIFI
            isEth  -> ConnectionType.ETHERNET
            else   -> ConnectionType.UNKNOWN
        }

        // IP local (IPv4) y máscara de subred
        val linkAddr = linkProps?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address && !it.address.isLoopbackAddress }
        val localIp = linkAddr?.address?.hostAddress
        val prefix = linkAddr?.prefixLength ?: 0
        val subnetMask = if (prefix > 0) {
            val mask = ((-1L) shl (32 - prefix)) and 0xFFFFFFFFL
            "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
        } else null

        // IPv6 (primera dirección global, no loopback)
        val ipv6 = linkProps?.linkAddresses
            ?.firstOrNull { it.address is Inet6Address && !it.address.isLoopbackAddress }
            ?.address?.hostAddress

        // Puerta de enlace predeterminada
        val gateway = linkProps?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway != null }
            ?.gateway?.hostAddress

        // Servidores DNS
        val dnsServers = linkProps?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        // Nombre de interfaz y MAC
        val ifaceName = linkProps?.interfaceName ?: "desconocida"
        val macAddress = try {
            NetworkInterface.getByName(ifaceName)?.hardwareAddress
                ?.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) { null }

        val mtu = linkProps?.mtu?.takeIf { it > 0 }

        // Información WiFi (SSID y señal) — requiere ACCESS_WIFI_STATE
        var ssid: String? = null
        var signalPercent: Int? = null
        var signalDbm: Int? = null

        if (isWifi) {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager?.connectionInfo
            val rawSsid = wifiInfo?.ssid
            ssid = if (rawSsid != null && rawSsid != "<unknown ssid>")
                rawSsid.removeSurrounding("\"") else null
            val rssi = wifiInfo?.rssi
            if (rssi != null && rssi != -1) {
                signalDbm = rssi
                @Suppress("DEPRECATION")
                signalPercent = WifiManager.calculateSignalLevel(rssi, 101).coerceIn(0, 100)
            }
        }

        // IP pública vía HTTP
        val publicIp = try {
            URL("https://api.ipify.org").readText().trim().takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }

        NetworkInfo(
            interfaceName  = ifaceName,
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
}
