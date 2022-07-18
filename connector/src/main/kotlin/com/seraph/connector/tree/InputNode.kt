package com.seraph.connector.tree

import com.seraph.connector.configuration.matchingType
import com.seraph.connector.tools.BlockingNetwork
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class InputNode<T : Any>(
    private val devId: String,
    private val endId: String,
    private val type: KClass<T>,
    private val network: BlockingNetwork,
    private val log: Log
) : Node {

    private val buffer = Channel<T>(1, BufferOverflow.DROP_OLDEST) {
        log.w("Value $it was dropped")
    }

    val consumer: Node.Consumer<T> = object : Node.Consumer<T> {
        override val parent: Node
            get() = this@InputNode

        override suspend fun consume(flow: StateFlow<T?>) {
            flow.filterNotNull().collect {
                buffer.send(it)
            }
        }
    }

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            val matchingType = matchingType(type)
            if (matchingType == null) {
                log.w("No matching type with $type")
                return@launch
            }

            var outputJob: Job? = null
            while (true) {
                val device = network.read(Device.Id(devId))
                outputJob?.cancel()
                val endpoint = device.endpoints.firstOrNull { it.id == Endpoint.Id(endId) }
                if (endpoint == null) {
                    log.w("Endpoint $devId/$endId not found")
                    continue
                }
                if (!validateEndpoint(endpoint)) {
                    continue
                }
                outputJob = scope.launch {
                    for (data in buffer) {
                        @Suppress("UNCHECKED_CAST")
                        network.publish(device.id, endpoint as Endpoint<T>, data)
                    }
                }
            }
        }
    }

    private fun validateEndpoint(endpoint: Endpoint<*>): Boolean {
        val matchingType = matchingType(type)
        if (endpoint.type != matchingType) {
            log.w(
                "Endpoint type mismatch. $devId/$endId has type ${endpoint.type}" +
                        ", but requested type is $matchingType\""
            )
            return false
        }
        if (endpoint.direction != Endpoint.Direction.INPUT) {
            log.w("Endpoint $devId/$endId should have INPUT direction instead of ${endpoint.direction}")
            return false
        }
        return true
    }
}