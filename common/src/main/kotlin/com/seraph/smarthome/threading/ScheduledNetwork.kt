package com.seraph.smarthome.threading

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo
import com.seraph.smarthome.domain.Network

class ScheduledNetwork(
        private val wrapped: Network,
        private val executor: Executor,
) : Network {

    override fun publish(metainfo: Metainfo): Network.Publication = wrapped.publish(metainfo)
    override fun publish(device: Device): Network.Publication = wrapped.publish(device)

    override fun <T> publish(
        device: Device.Id,
        endpoint: Endpoint<T>,
        data: T
    ): Network.Publication = wrapped.publish(device, endpoint, data)

    override fun subscribe(func: (Metainfo) -> Unit): Network.Subscription {
        return wrapped.subscribe { meta ->
            executor.run { func(meta) }
        }
    }

    override fun subscribe(device: Device.Id?, func: (Device) -> Unit): Network.Subscription {
        return wrapped.subscribe(device) { dev ->
            executor.run { func(dev) }
        }
    }

    override fun <T> subscribe(
            device: Device.Id,
            endpoint: Endpoint<T>,
            func: (Device.Id, Endpoint<T>, data: T) -> Unit,
    ): Network.Subscription {

        return wrapped.subscribe(device, endpoint) { dev, end, dat ->
            executor.run { func(dev, end, dat) }
        }
    }

    override var statusListener: Network.StatusListener
        get() = wrapped.statusListener
        set(value) {
            wrapped.statusListener = object : Network.StatusListener {
                override fun onStatusChanged(status: Network.Status) {
                    executor.run { value.onStatusChanged(status) }
                }
            }
        }
}