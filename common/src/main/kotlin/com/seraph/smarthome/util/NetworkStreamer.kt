package com.seraph.smarthome.util

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network

class NetworkStreamer(
        private val network: Network,
        private val log: Log = NoLog(),
        private val recordEvents: Boolean,
        private val listener: (NetworkStreamer) -> Unit
) {
    private val state = NetworkState(mutableMapOf(), mutableListOf())

    val snapshot: NetworkSnapshot
        get() {
            synchronized(this) {
                return state.snapshot
            }
        }

    val events: List<NetworkEvent>
        get() {
            synchronized(this) {
                val events = state.events
                state.events = mutableListOf()
                return events
            }
        }

    init {
        network.subscribe(null) {
            handleDevice(it)
        }
    }

    private inline fun NetworkState.recordEvent(eventAdder: () -> NetworkEvent) {
        if (recordEvents) {
            events.add(eventAdder())
        }
    }

    private inline fun <T> modifyState(updater: (NetworkState) -> T): T {
        try {
            synchronized(this) {
                return updater(state)
            }
        } finally {
            listener(this)
        }
    }

    private fun handleDevice(device: Device) = modifyState { state ->
        val newDeviceEvent = state.devices[device.id] == null
        val oldDevice = state.devices[device.id] ?: DeviceState(device, mutableMapOf())
        val newDevice = makeNewDeviceState(device, oldDevice)
        state.devices[device.id] = newDevice

        if (newDeviceEvent) {
            state.recordEvent { NetworkEvent.DeviceAdded(newDevice.snapshot) }
        } else {
            state.recordEvent { NetworkEvent.DeviceUpdated(newDevice.snapshot) }
        }

        (oldDevice.endpoints.keys + newDevice.endpoints.keys).forEach { id ->
            val old = oldDevice.endpoints[id]
            val new = newDevice.endpoints[id]

            if (old == null && new != null) {
                new.subscription = subscribeEndpoint(device, new)
                state.recordEvent { NetworkEvent.EndpointAdded(new.snapshot) }
            } else if (old != null && new == null) {
                old.subscription?.unsubscribe()
                state.recordEvent { NetworkEvent.EndpointRemoved(old.snapshot) }
            } else if (old != null && new != null && old.endpoint.type != new.endpoint.type) {
                old.subscription?.unsubscribe()
                new.subscription = subscribeEndpoint(device, new)
                state.recordEvent { NetworkEvent.EndpointUpdated(new.snapshot) }
            }
        }
    }

    private fun <T> subscribeEndpoint(
            device: Device,
            endpointState: EndpointState<T>): Network.Subscription {
        return network.subscribe(device.id, endpointState.endpoint) { _, _, v ->
            modifyState { state ->
                endpointState.isSet = true
                endpointState.value = v
                state.recordEvent { NetworkEvent.EndpointUpdated(endpointState.snapshot) }
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
        val devices: Map<Device.Id, DeviceSnapshot>,
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
        val devices: MutableMap<Device.Id, DeviceState>,
        var events: MutableList<NetworkEvent>
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

sealed class NetworkEvent {
    data class EndpointUpdated<T>(val endpoint: EndpointSnapshot<T>) : NetworkEvent()
    data class EndpointAdded<T>(val endpoint: EndpointSnapshot<T>) : NetworkEvent()
    data class EndpointRemoved<T>(val endpoint: EndpointSnapshot<T>) : NetworkEvent()
    data class DeviceAdded(val device: DeviceSnapshot) : NetworkEvent()
    data class DeviceUpdated(val device: DeviceSnapshot) : NetworkEvent()
}