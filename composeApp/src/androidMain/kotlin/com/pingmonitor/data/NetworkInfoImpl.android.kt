package com.pingmonitor.data

import com.pingmonitor.domain.ConnectionType
import com.pingmonitor.domain.NetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

actual class NetworkInfoImpl actual constructor() : NetworkInfoRepository {

    override suspend fun getNetworkInfo(): NetworkInfo? = withContext(Dispatchers.IO) {
        val iface = NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.filter { !it.isLoopback && it.isUp }
            ?.firstOrNull() ?: return@withContext null

        val ifAddr = iface.interfaceAddresses
            .firstOrNull { it.address is Inet4Address } ?: return@withContext null

        val prefix = ifAddr.networkPrefixLength
        val mask = if (prefix <= 0) 0L else ((-1L) shl (32 - prefix)) and 0xFFFFFFFFL
        val subnetMask = "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"

        NetworkInfo(
            interfaceName  = iface.displayName ?: iface.name,
            connectionType = ConnectionType.UNKNOWN,
            localIp        = ifAddr.address.hostAddress,
            subnetMask     = subnetMask,
            gateway        = null,
            ipv6           = iface.interfaceAddresses
                .firstOrNull { it.address is Inet6Address && !it.address.isLoopbackAddress }
                ?.address?.hostAddress,
            publicIp       = null,
            dnsServers     = emptyList(),
            macAddress     = iface.hardwareAddress?.joinToString(":") { "%02X".format(it) },
            mtu            = iface.mtu.takeIf { it > 0 },
            ssid           = null,
            signalPercent  = null,
            signalDbm      = null
        )
    }
}
