package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttException

class DisconnectingState(exchanger: Exchanger) : BaseState(exchanger) {
    override fun engage() = transact { data ->
        try {
            data.client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    transact { data.copy(state = DisconnectedState(exchanger)) }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    transact { data.copy(state = DisconnectedState(exchanger)) }
                }
            })
            data
        } catch (ex: MqttException) {
            data.copy(state = DisconnectedState(exchanger))
        }
    }

    override fun disengage(data: SharedData) = Unit

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onDisconnectedState()

    override fun execute(action: (MqttAsyncClient) -> Unit) {
        throw OperationDeclinedException("Client is disconnecting")
    }
}