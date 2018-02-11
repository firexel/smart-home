package com.seraph.smarthome.domain

/**
 * Created by aleksandr.naumov on 17.01.18.
 */

interface Network {

    fun publish(metainfo: Metainfo)
    fun subscribe(func: (Metainfo) -> Unit)

    fun publish(device: Device)
    fun subscribe(device: Device.Id?, func: (Device) -> Unit)

    fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T)
    fun <T> subscribe(device: Device.Id, endpoint: Endpoint<T>, func: (Device.Id, Endpoint<T>, data: T) -> Unit)
}
