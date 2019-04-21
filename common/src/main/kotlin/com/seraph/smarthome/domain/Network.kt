package com.seraph.smarthome.domain

/**
 * Created by aleksandr.naumov on 17.01.18.
 */

interface Network {

    fun publish(metainfo: Metainfo): Publication
    fun subscribe(func: (Metainfo) -> Unit)

    fun publish(device: Device): Publication
    fun subscribe(device: Device.Id?, func: (Device) -> Unit)

    fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T): Publication
    fun <T> subscribe(device: Device.Id, endpoint: Endpoint<T>, func: (Device.Id, Endpoint<T>, data: T) -> Unit)

    var statusListener: StatusListener

    interface Publication {
        fun waitForCompletion(millis: Long)
    }

    interface StatusListener {
        fun onStatusChanged(status: Status)
    }

    enum class Status {
        ONLINE, OFFLINE
    }
}
