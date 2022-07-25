@file:OptIn(ExperimentalCoroutinesApi::class)

package com.seraph.connector.tree

import com.seraph.connector.configuration.matchingType
import com.seraph.connector.tools.BlockingNetwork
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class OutputNode<T : Any>(
    private val devId: String,
    private val endId: String,
    private val type: KClass<T>,
    private val network: BlockingNetwork,
    private val log: Log
) : Node {

    private val flow = MutableStateFlow<T?>(null)

    val producer: Node.Producer<T> = object : Node.Producer<T> {
        override val parent: Node
            get() = this@OutputNode
        override val flow: Flow<T?>
            get() = this@OutputNode.flow
    }

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            val matchingType = matchingType(type)
            if (matchingType == null) {
                log.w("No matching type with $type")
                return@launch
            }

            network.read(Device.Id(devId))
                .flatMapLatest { device ->
                    val endpoint = device.endpoints.firstOrNull { it.id == Endpoint.Id(endId) }
                    if (endpoint == null) {
                        log.w("Endpoint $devId/$endId not found")
                        flow {}
                    } else if (!validateEndpoint(endpoint)) {
                        flow {}
                    } else {
                        network.read(Device.Id(devId), endpoint)
                    }
                }
                .flowOn(scope.coroutineContext)
                .collect { value ->
                    @Suppress("UNCHECKED_CAST")
                    flow.value = value as T
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
        if (endpoint.direction != Endpoint.Direction.OUTPUT) {
            log.w("Endpoint $devId/$endId should have OUTPUT direction instead of ${endpoint.direction}")
            return false
        }
        return true
    }
}