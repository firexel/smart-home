package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker

internal class ConnectedState(exchanger: Exchanger<SharedData>) : BaseState(exchanger) {

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
        val successfulActions = mutableListOf<(Client) -> Unit>()
        return try {
            data.actions.forEach {
                it(data.client)
                successfulActions.add(it)
            }
            data.copy(actions = emptyList())
        } catch (ex: ClientException) {
            val state = inferNextState(ex)
            val actions = if (state is WaitingState) data.actions - successfulActions else emptyList()
            data.copy(state = state, actions = actions)
        }
    }

    private fun inferNextState(ex: ClientException): State = when (ex.reason) {
        ClientException.Reason.BAD_BROKER_STATE,
        ClientException.Reason.BAD_NETWORK,
        ClientException.Reason.BAD_CLIENT_LOGIC
        -> WaitingState(exchanger)

        ClientException.Reason.BAD_CLIENT_STATE,
        ClientException.Reason.BAD_CLIENT_CONFIGURATION
        -> DisconnectingState(exchanger)
    }

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onConnectedState()

    override fun execute(action: (Client) -> Unit) = transact { data ->
        try {
            action(data.client)
            data
        } catch (ex: ClientException) {
            val state = inferNextState(ex)
            val actions = if (state is WaitingState) data.actions + action else emptyList()
            data.copy(state = state, actions = actions)
        }
    }
}