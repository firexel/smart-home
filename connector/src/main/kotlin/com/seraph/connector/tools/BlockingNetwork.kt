package com.seraph.connector.tools

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import kotlinx.coroutines.flow.Flow

interface BlockingNetwork {
    suspend fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T)
    fun <T> read(device: Device.Id, endpoint: Endpoint<T>): Flow<T>
    fun read(device: Device.Id): Flow<Device>
}