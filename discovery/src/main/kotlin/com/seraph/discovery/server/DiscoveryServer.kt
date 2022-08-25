package com.seraph.discovery.server

import com.seraph.discovery.model.FacilityInfoRequest
import com.seraph.discovery.model.FacilityInfoResponse
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.URL

class DiscoveryServer(
    private val response: FacilityInfoResponse,
    private val log: Log
) {
    private val port = 1705

    suspend fun serve() = withContext(Dispatchers.IO) {
        while (isActive) {
            var socket: DatagramSocket? = null
            try {
                log.v("Start receiving on port $port")
                socket = runInterruptible { DatagramSocket(port) }
                socket.broadcast = true
                while (isActive) {
                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)
                    runInterruptible { socket.receive(packet) }
                    log.i("Received ${packet.length} bytes from ${packet.address.hostAddress}")
                    launch { handleRequest(packet) }
                }
            } catch (ex: IOException) {
                log.w("Receiving interrupted due to $ex")
                runInterruptible { if (socket?.isClosed == true) socket.close() }
                delay(1000L)
            }
        }
    }

    private suspend fun handleRequest(packet: DatagramPacket) {
        try {
            val addr = readRequest(packet)
            runInterruptible { doCallback(addr.host, addr.port) }
        } catch (ex: Throwable) {
            log.w("Error processing request from ${packet.address.hostAddress}: $ex")
        }
    }

    private fun doCallback(host: String, port: Int) {
        val data = Json.encodeToString(response).toByteArray(Charsets.UTF_8)
        val url = URL("http://$host:$port")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Content-Length", "${data.size}")
        conn.connectTimeout = 3000
        DataOutputStream(conn.outputStream).use { it.write(data) }
        BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
            var response = ""
            var line: String?
            while (br.readLine().also { line = it } != null) {
                response += line
            }
            log.v("Response sent to $url. Requester response is:\n$response")
        }
    }

    private fun readRequest(packet: DatagramPacket): RequestData {
        val string = String(packet.data.copyOf(packet.length), Charsets.UTF_8).trim()
        try {
            val request: FacilityInfoRequest = Json.decodeFromString(string)
            return RequestData(packet.address.hostAddress, request.port)
        } catch (ex: SerializationException) {
            throw RuntimeException("Error reading json from ${packet.address.hostAddress}. \nReason: ${ex.message}. \nRequest is '$string'", ex)
        }
    }

    private data class RequestData(
        val host: String,
        val port: Int
    )
}