package com.seraph.connector.tools

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint

interface BlockingNetwork {
    suspend fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T)
    suspend fun <T> read(device: Device.Id, endpoint: Endpoint<T>): T
    suspend fun read(device: Device.Id): Device
}