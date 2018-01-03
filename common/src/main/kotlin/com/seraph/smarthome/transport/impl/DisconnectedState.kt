package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import org.eclipse.paho.client.mqttv3.MqttAsyncClient

class DisconnectedState(exchanger: Exchanger) : BaseState(exchanger) {
    override fun engage() = Unit

    override fun disengage(data: SharedData) = Unit

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onDisconnectedState()

    override fun execute(action: (MqttAsyncClient) -> Unit) {
        throw OperationDeclinedException("Client is disconnected")
    }
}