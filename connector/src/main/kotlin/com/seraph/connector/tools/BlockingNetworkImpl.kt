package com.seraph.connector.tools

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

class BlockingNetworkImpl(
    private val network: Network,
    private val log: Log
) : BlockingNetwork {

    override suspend fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T) {
        network.publish(device, endpoint, data)
    }

    override fun <T> read(device: Device.Id, endpoint: Endpoint<T>): Flow<T> {
        return callbackFlow {
            val sub = network.subscribe(device, endpoint) { _, _, data ->
                trySendBlocking(data)
                    .onFailure { log.w("Data send failure") }
            }
            awaitClose {
                log.v("Closed data reading")
                sub.unsubscribe()
            }
        }
    }

    override fun read(device: Device.Id): Flow<Device> {
        return callbackFlow {
            val sub = network.subscribe(device) { device ->
                trySendBlocking(device)
                    .onFailure { log.w("Device structure send failure") }
            }
            awaitClose {
                log.v("Closed device reading")
                sub.unsubscribe()
            }
        }.buffer(1)
    }
}