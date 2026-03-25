package com.pingmonitor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

actual class SpeedTesterImpl actual constructor() : SpeedTesterRepository {

    override suspend fun measurePing(host: String): Double? = try {
        val addr = InetAddress.getByName(host)
        val start = System.currentTimeMillis()
        if (addr.isReachable(3000)) System.currentTimeMillis() - start.toDouble() else null
    } catch (_: Exception) { null }

    override fun measureDownload(): Flow<Double> = flow {
        val url = URL("https://speed.cloudflare.com/__down?bytes=25000000")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout   = 30_000
        }
        connection.connect()
        val buffer    = ByteArray(32_768)
        var totalBytes = 0L
        val startTime  = System.currentTimeMillis()
        connection.inputStream.use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                totalBytes += read
                val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                emit((totalBytes * 8.0) / (elapsed / 1000.0) / 1_000_000.0)
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun measureUpload(): Flow<Double> = flow {
        val targetBytes = 10_000_000L
        val url = URL("https://speed.cloudflare.com/__up")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput      = true
            connectTimeout = 5_000
            readTimeout    = 30_000
            setFixedLengthStreamingMode(targetBytes)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        connection.connect()
        val data       = ByteArray(32_768)
        var totalBytes = 0L
        val startTime  = System.currentTimeMillis()
        connection.outputStream.use { stream ->
            while (totalBytes < targetBytes) {
                val toWrite = minOf(data.size.toLong(), targetBytes - totalBytes).toInt()
                stream.write(data, 0, toWrite)
                totalBytes += toWrite
                val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                emit((totalBytes * 8.0) / (elapsed / 1000.0) / 1_000_000.0)
            }
        }
    }.flowOn(Dispatchers.IO)
}
