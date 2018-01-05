package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker

internal class ConnectingState(exchanger: Exchanger<SharedData>) : BaseState(exchanger) {

    override fun engage() = transact { data ->
        try {
            data.client.connect(
                    onSuccess = {
                        transact { data ->
                            data.copy(state = ConnectedState(exchanger), timesRetried = 0)
                        }
                    },
                    onFail = { error ->
                        transact { data ->
                            val nextState = inferNextState(error)
                            data.copy(state = nextState)
                        }
                    }
            )
            data
        } catch (ex: ClientException) {
            data.copy(state = inferNextState(ex))
        }
    }

    override fun disengage() = Unit

    override fun <T> accept(visitor: Broker.BrokerState.Visitor<T>): T = visitor.onConnectingState()

    override fun execute(key: Any?, action: (Client) -> Unit) = transact {
        it.copy(actions = it.actions + SharedData.Action(key, action))
    }

    private fun inferNextState(exception: ClientException) = when (exception.reason) {
        ClientException.Reason.BAD_BROKER_STATE,
        ClientException.Reason.BAD_CLIENT_STATE,
        ClientException.Reason.BAD_NETWORK
        -> WaitingState(exchanger)

        ClientException.Reason.BAD_CLIENT_CONFIGURATION,
        ClientException.Reason.BAD_CLIENT_LOGIC
        -> DisconnectedState(exchanger)
    }
}