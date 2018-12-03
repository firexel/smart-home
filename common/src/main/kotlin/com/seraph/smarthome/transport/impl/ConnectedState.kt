package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.util.State

internal class ConnectedState(exchanger: Exchanger<BaseState, SharedData>) : BaseState(exchanger) {

    override fun engage() = transact { data ->
        data.client.disconnectionCallback = { cause ->
            transact { it.copy(state = inferNextState(cause)) }
        }
        performStoredActions(data)
    }

    override fun disengage() = sync {
        it.client.disconnectionCallback = null
    }

    private fun performStoredActions(data: SharedData): SharedData {
        val successfulActions = mutableListOf<SharedData.Action>()
        return try {
            data.actions.forEach {
                it.lambda(data.client)
                successfulActions.add(it)
            }
            data.copy(actions = data.actions.filter { it.persisted })
        } catch (ex: ClientException) {
            val state = inferNextState(ex)
            val actions = if (state is WaitingState) {
                data.actions - successfulActions.filter { it.singleUse }
            } else {
                emptyList()
            }
            data.copy(state = state, actions = actions)
        }
    }

    private fun inferNextState(ex: ClientException): BaseState = when (ex.reason) {
        ClientException.Reason.BAD_BROKER_STATE,
        ClientException.Reason.BAD_NETWORK,
        ClientException.Reason.BAD_CLIENT_LOGIC
        -> WaitingState(exchanger)

        ClientException.Reason.BAD_CLIENT_STATE,
        ClientException.Reason.BAD_CLIENT_CONFIGURATION
        -> DisconnectingState(exchanger)
    }

    override fun <T> accept(visitor: Broker.BrokerState.Visitor<T>): T = visitor.onConnectedState()

    override fun execute(key: Any?, action: (Client) -> Unit) = transact { data ->
        try {
            action(data.client)
            val actionObject = SharedData.Action(key, action)
            if (actionObject.persisted) {
                data.copy(actions = data.actions + actionObject)
            } else {
                data
            }
        } catch (ex: ClientException) {
            val state = inferNextState(ex)
            val actions = if (state is WaitingState) {
                data.actions + SharedData.Action(key, action)
            } else {
                emptyList()
            }
            data.copy(state = state, actions = actions)
        }
    }
}