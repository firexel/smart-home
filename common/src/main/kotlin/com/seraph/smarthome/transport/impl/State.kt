package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import org.eclipse.paho.client.mqttv3.MqttAsyncClient

interface State {
    fun engage()
    fun disengage(data: SharedData)

    fun <T> accept(visitor: Broker.Visitor<T>): T
    fun execute(action: (MqttAsyncClient) -> Unit)
}