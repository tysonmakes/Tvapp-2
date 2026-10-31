package com.tysonmakes.tvremoteapp.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

data class ScanProgress(
    val currentIp: String,
    val scannedCount: Int,
    val totalCount: Int,
    val foundDevices: List<String>,
    val isComplete: Boolean = false
)

object DeviceScanner {

    fun getLocalSubnetPrefix(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress ?: continue
                        val lastDot = hostAddress.lastIndexOf('.')
                        if (lastDot != -1) {
                            return hostAddress.substring(0, lastDot + 1)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.1."
    }

    suspend fun isPortOpen(ip: String, port: Int = 5555, timeoutMs: Int = 250): Boolean = coroutineScope {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun scanSubnet(
        subnetPrefix: String = getLocalSubnetPrefix(),
        port: Int = 5555,
        startHost: Int = 1,
        endHost: Int = 254
    ): Flow<ScanProgress> = flow {
        val safePrefix = if (subnetPrefix.endsWith(".")) subnetPrefix else "$subnetPrefix."
        val found = mutableListOf<String>()
        val total = endHost - startHost + 1
        var scanned = 0

        // Process in batches to balance speed and network resource limits
        val batchSize = 25
        for (i in startHost..endHost step batchSize) {
            val currentEnd = minOf(i + batchSize - 1, endHost)
            val batchIps = (i..currentEnd).map { "$safePrefix$it" }

            val batchResults = coroutineScope {
                batchIps.map { ip ->
                    async(Dispatchers.IO) {
                        val open = isPortOpen(ip, port, timeoutMs = 280)
                        if (open) ip else null
                    }
                }.awaitAll()
            }

            for (res in batchResults) {
                if (res != null && !found.contains(res)) {
                    found.add(res)
                }
            }
            scanned += batchIps.size

            emit(
                ScanProgress(
                    currentIp = "$safePrefix$currentEnd",
                    scannedCount = scanned,
                    totalCount = total,
                    foundDevices = found.toList(),
                    isComplete = false
                )
            )
        }

        emit(
            ScanProgress(
                currentIp = "",
                scannedCount = total,
                totalCount = total,
                foundDevices = found.toList(),
                isComplete = true
            )
        )
    }.flowOn(Dispatchers.IO)
}
