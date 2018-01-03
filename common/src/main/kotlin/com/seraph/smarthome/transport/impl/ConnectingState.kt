package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

class ConnectingState(exchanger: Exchanger) : BaseState(exchanger) {

    override fun engage() = sync { data ->
        data.client.connect(createOptions(), null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) = transact { data ->
                data.copy(state = ConnectedState(exchanger), timesRetried = 0)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) = transact { data ->
                val nextState = when (MqttOperationException(exception).reason) {
                    MqttOperationException.Reason.BAD_BROKER_STATE,
                    MqttOperationException.Reason.BAD_CLIENT_STATE,
                    MqttOperationException.Reason.BAD_NETWORK
                    -> WaitingState(exchanger)

                    MqttOperationException.Reason.BAD_CLIENT_CONFIGURATION,
                    MqttOperationException.Reason.BAD_CLIENT_LOGIC
                    -> DisconnectedState(exchanger)
                }
                data.copy(state = nextState)
            }
        })
    }

    override fun disengage(data: SharedData) = Unit

    private fun createOptions(): MqttConnectOptions = MqttConnectOptions().apply {
        isAutomaticReconnect = false
        keepAliveInterval = 10
    }

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onConnectingState()

    override fun execute(action: (MqttAsyncClient) -> Unit) = transact {
        it.copy(actions = it.actions + action)
    }
}