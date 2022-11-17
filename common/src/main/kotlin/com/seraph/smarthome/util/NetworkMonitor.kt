package com.seraph.smarthome.util

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo
import com.seraph.smarthome.domain.Network
import java.util.concurrent.CopyOnWriteArrayList

class NetworkMonitor(
    private val network: Network,
    private val log: Log = NoLog(),
    private val recordEvents: Boolean
) {
    private val subscriptions = CopyOnWriteArrayList<SubscriptionImpl>()
    private var started: Boolean = false

    private val state = NetworkState(
        Metainfo("Unknown", Metainfo.Role.USER, listOf()),
        mutableMapOf(),
        mutableListOf()
    )

    val events: List<NetworkEvent>
        get() {
            synchronized(this) {
                val events = state.events
                state.events = mutableListOf()
                return events
            }
        }

    fun subscribe(listener: (NetworkMonitor) -> Unit): Subscription {
        val sub = SubscriptionImpl(listener)
        subscriptions.add(sub)
        start()
        return sub
    }

    fun start() {
        if (!started) {
            started = true
            network.subscribe(null) {
                handleDevice(it)
            }
            network.subscribe {
                handleMetainfo(it)
            }
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
            subscriptions.forEach { it.listener(this) }
        }
    }

    private inline fun <T> readState(updater: (NetworkState) -> T): T {
        synchronized(this) {
            return updater(state)
        }
    }

    private fun handleMetainfo(info: Metainfo) = modifyState { state ->
        state.metainfo = info
        state.recordEvent { NetworkEvent.MetainfoUpdated(info) }
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
        endpointState: EndpointState<T>
    ): Network.Subscription {
        return network.subscribe(device.id, endpointState.endpoint) { _, _, v ->
            modifyState { state ->
                endpointState.isSet = true
                endpointState.value = v
                endpointState.timeSet = System.currentTimeMillis()
                state.recordEvent { NetworkEvent.EndpointUpdated(endpointState.snapshot) }
            }
        }
    }

    private fun makeNewDeviceState(device: Device, oldDevice: DeviceState): DeviceState {
        val newEndpoints = device.endpoints.map { endpoint ->
            val oldEndpointState = oldDevice.endpoints[endpoint.id]
            if (oldEndpointState != null
                && oldEndpointState.endpoint.type == endpoint.type
            ) {
                oldEndpointState
            } else if (oldEndpointState == null) {
                EndpointState(device, endpoint, null, false, null, 0L)
            } else {
                log.w(
                    "New endpoint ${device.id}:${endpoint.id} has a different type with the " +
                            "previous one: ${oldEndpointState.endpoint.type} -> ${endpoint.type}"
                )

                EndpointState(device, endpoint, null, false, null, 0L)
            }
        }

        val endpoints = newEndpoints
            .associateBy { it.endpoint.id }
            .toMutableMap()

        return DeviceState(device, endpoints)
    }

    fun snapshot(deviceId: Device.Id, endpointId: Endpoint.Id): EndpointSnapshot<*>? {
        return readState { state ->
            state.devices[deviceId]?.endpoints?.get(endpointId)?.snapshot
        }
    }

    fun snapshot(deviceId: Device.Id): DeviceSnapshot? {
        return readState { state ->
            state.devices[deviceId]?.snapshot
        }
    }

    fun snapshot(): NetworkSnapshot {
        return readState {
            state.snapshot
        }
    }

    interface Subscription {
        fun unsubscribe()
    }

    private inner class SubscriptionImpl(val listener: (NetworkMonitor) -> Unit) : Subscription {
        override fun unsubscribe() {
            subscriptions.remove(this)
        }
    }
}

data class NetworkSnapshot(
    val metainfo: Metainfo,
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
    val isSet: Boolean,
    val timeSet: Long
)

private data class NetworkState(
    var metainfo: Metainfo,
    val devices: MutableMap<Device.Id, DeviceState>,
    var events: MutableList<NetworkEvent>
) {
    val snapshot: NetworkSnapshot
        get() = NetworkSnapshot(metainfo, devices.mapValues { it.value.snapshot })
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
    var subscription: Network.Subscription? = null,
    var timeSet: Long
) {
    val snapshot: EndpointSnapshot<T>
        get() = EndpointSnapshot(device, endpoint, value, isSet, timeSet)
}

sealed class NetworkEvent {
    data class EndpointUpdated<T>(val endpoint: EndpointSnapshot<T>) : NetworkEvent()
    data class EndpointAdded<T>(val endpoint: EndpointSnapshot<T>) : NetworkEvent()
    data class EndpointRemoved<T>(val endpoint: EndpointSnapshot<T>) : NetworkEvent()
    data class DeviceAdded(val device: DeviceSnapshot) : NetworkEvent()
    data class DeviceUpdated(val device: DeviceSnapshot) : NetworkEvent()
    data class MetainfoUpdated(val metainfo: Metainfo) : NetworkEvent()
}