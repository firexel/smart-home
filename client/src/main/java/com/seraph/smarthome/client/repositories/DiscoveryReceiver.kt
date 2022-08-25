package com.seraph.smarthome.client.repositories

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DiscoveryReceiver {

    private val port = 1706

    fun collectDiscoveryAdvertises(timeoutMs: Int): Flow<FacilityInfoAdvertise> =
        callbackFlow {
            val server = defineServer {
                trySendBlocking(it)
            }
            server.environment.monitor.subscribe(ApplicationStarted) {
                sendBroadcast()
            }
            server.start(wait = false)
            delay(timeoutMs.toLong())
            close()
            awaitClose {
                server.stop()
            }
        }

    private fun sendBroadcast() {
        val bytes = Json.encodeToString(FacilityInfoBroadcast(port))
            .toByteArray(Charsets.UTF_8)

        CoroutineScope(Dispatchers.IO).launch {
            delay(200)
            runInterruptible {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val packet = DatagramPacket(
                        bytes,
                        bytes.size,
                        InetAddress.getByName("255.255.255.255"),
                        1705
                    )
                    socket.send(packet)
                }
            }
        }
    }

    private fun defineServer(consumer: (FacilityInfoAdvertise) -> Unit) =
        embeddedServer(Netty, port = port) {
            routing {
                install(ContentNegotiation) {
                    json()
                }
                post("") {
                    val newConfig = call.receiveText()
                    try {
                        val advertise: FacilityInfoAdvertise = Json.decodeFromString(newConfig)
                        call.respond(HttpStatusCode.OK, "Received")
                        consumer(advertise)
                    } catch (ex: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Cannot parse request as FacilityInfoAdvertise"
                        )
                    }
                }
            }
        }
}

@Serializable
data class FacilityInfoAdvertise(
    val id: String,
    val name: String,
    val imageUrl: String,
    val brokerHost: String,
    val brokerPort: Int,
    val brokerLogin: String?,
    val brokerPassword: String?
)


@Serializable
data class FacilityInfoBroadcast(
    val port: Int
)