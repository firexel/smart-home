package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import org.eclipse.paho.client.mqttv3.*

class ConnectedState(exchanger: Exchanger) : BaseState(exchanger) {

    override fun engage() = transact { data ->
        signAsListener(data.client)
        performStoredActions(data)
    }

    override fun disengage(data: SharedData) {
        data.client.setCallback(null)
    }

    private fun signAsListener(client: MqttAsyncClient) {
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                transact { it.copy(state = inferNextState(MqttOperationException(cause))) }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) = Unit
            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })
    }

    private fun performStoredActions(data: SharedData): SharedData {
        val successfulActions = mutableListOf<(MqttAsyncClient) -> Unit>()
        return try {
            data.actions.forEach {
                performAction(it, data.client)
                successfulActions.add(it)
            }
            data.copy(actions = emptyList())
        } catch (ex: MqttOperationException) {
            data.copy(
                    state = inferNextState(ex),
                    actions = data.actions - successfulActions
            )
        }
    }

    private fun inferNextState(ex: MqttOperationException): State = when (ex.reason) {
        MqttOperationException.Reason.BAD_BROKER_STATE,
        MqttOperationException.Reason.BAD_NETWORK,
        MqttOperationException.Reason.BAD_CLIENT_LOGIC
        -> WaitingState(exchanger)

        MqttOperationException.Reason.BAD_CLIENT_STATE,
        MqttOperationException.Reason.BAD_CLIENT_CONFIGURATION
        -> DisconnectingState(exchanger)
    }

    private fun performAction(action: (MqttAsyncClient) -> Unit, client: MqttAsyncClient) {
        try {
            action(client)
        } catch (ex: MqttException) {
            throw MqttOperationException(ex)
        }
    }

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onConnectedState()

    override fun execute(action: (MqttAsyncClient) -> Unit) = transact { data ->
        try {
            performAction(action, data.client)
            data
        } catch (ex: MqttOperationException) {
            data.copy(
                    state = inferNextState(ex),
                    actions = data.actions + action
            )
        }
    }
}