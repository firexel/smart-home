package com.seraph.smarthome.domain

/**
 * Created by aleksandr.naumov on 17.01.18.
 */

interface Network {

    fun publish(metainfo: Metainfo): Publication
    fun subscribe(func: (Metainfo) -> Unit):Subscription

    fun publish(device: Device): Publication
    fun subscribe(device: Device.Id?, func: (Device) -> Unit):Subscription

    fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T): Publication
    fun <T> subscribe(device: Device.Id, endpoint: Endpoint<T>, func: (Device.Id, Endpoint<T>, data: T) -> Unit): Subscription

    var statusListener: StatusListener

    interface Publication {
        fun waitForCompletion(millis: Long)
    }

    interface Subscription {
        fun unsubscribe()
    }

    interface StatusListener {
        fun onStatusChanged(status: Status)
    }

    enum class Status {
        ONLINE, OFFLINE
    }
}
