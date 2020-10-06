package com.seraph.smarthome.util

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network

class NetworkStreamer(
        private val network: Network,
        private val log: Log = NoLog(),
        private val listener: (NetworkStreamer) -> Unit
) {
    private val state = NetworkState(mutableMapOf())

    val snapshot: NetworkSnapshot
        get() {
            synchronized(this) {
                return state.snapshot
            }
        }

    init {
        network.subscribe(null) {
            handleDevice(it)
        }
    }

    private inline fun <T> modifySnapshot(updater: (NetworkState) -> T): T {
        try {
            synchronized(this) {
                return updater(state)
            }
        } finally {
            listener(this)
        }
    }

    private fun handleDevice(device: Device) = modifySnapshot { snapshot ->
        val oldDevice = snapshot.devices[device.id] ?: DeviceState(device, mutableMapOf())
        val newDevice = makeNewDeviceState(device, oldDevice)
        snapshot.devices[device.id] = newDevice

        (oldDevice.endpoints.keys + newDevice.endpoints.keys).forEach { id ->
            val old = oldDevice.endpoints[id]
            val new = newDevice.endpoints[id]

            if (old == null && new != null) {
                new.subscription = subscribeEndpoint(device, new)
            } else if (old != null && new == null) {
                old.subscription?.unsubscribe()
            } else if (old != null && new != null && old.endpoint.type != new.endpoint.type) {
                old.subscription?.unsubscribe()
                new.subscription = subscribeEndpoint(device, new)
            }
        }
    }

    private fun <T> subscribeEndpoint(
            device: Device,
            endpointState: EndpointState<T>): Network.Subscription {
        return network.subscribe(device.id, endpointState.endpoint) { _, _, v ->
            modifySnapshot {
                endpointState.isSet = true
                endpointState.value = v
            }
        }
    }

    private fun makeNewDeviceState(device: Device, oldDevice: DeviceState): DeviceState {
        val newEndpoints = device.endpoints.map { endpoint ->
            val oldEndpointState = oldDevice.endpoints[endpoint.id]
            if (oldEndpointState != null
                    && oldEndpointState.endpoint.type == endpoint.type) {
                oldEndpointState
            } else if (oldEndpointState == null) {
                EndpointState(device, endpoint, null, false)
            } else {
                log.w("New endpoint ${device.id}:${endpoint.id} has a different type with the " +
                        "previous one: ${oldEndpointState.endpoint.type} -> ${endpoint.type}")

                EndpointState(device, endpoint, null, false)
            }
        }

        val endpoints = newEndpoints
                .associateBy { it.endpoint.id }
                .toMutableMap()

        return DeviceState(device, endpoints)
    }
}

data class NetworkSnapshot(
        val devices: Map<Device.Id, DeviceSnapshot>
)

data class DeviceSnapshot(
        val device: Device,
        val endpoints: Map<Endpoint.Id, EndpointSnapshot<*>>
)

data class EndpointSnapshot<T>(
        val device: Device,
        val endpoint: Endpoint<T>,
        val value: T?,
        val isSet: Boolean
)

private data class NetworkState(
        val devices: MutableMap<Device.Id, DeviceState>
) {
    val snapshot: NetworkSnapshot
        get() = NetworkSnapshot(devices.mapValues { it.value.snapshot })
}

private data class DeviceState(
        val device: Device,
        val endpoints: MutableMap<Endpoint.Id, EndpointState<*>>
) {
    val snapshot: DeviceSnapshot
        get() = DeviceSnapshot(device, endpoints.mapValues { it.value.snapshot })
}

private data class EndpointState<T>(
        val device: Device,
        val endpoint: Endpoint<T>,
        var value: T?,
        var isSet: Boolean,
        var subscription: Network.Subscription? = null
) {
    val snapshot: EndpointSnapshot<T>
        get() = EndpointSnapshot(device, endpoint, value, isSet)
}