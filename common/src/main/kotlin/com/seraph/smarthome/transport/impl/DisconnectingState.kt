package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker

class DisconnectingState(exchanger: Exchanger<SharedData>) : BaseState(exchanger) {
    override fun engage() = transact { data ->
        try {
            data.client.disconnect(
                    onSuccess = {
                        transact { data.copy(state = DisconnectedState(exchanger)) }
                    },
                    onFail = {
                        transact { data.copy(state = DisconnectedState(exchanger)) }
                    }
            )
            data
        } catch (ex: ClientException) {
            data.copy(state = DisconnectedState(exchanger))
        }
    }

    override fun disengage() = Unit

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onDisconnectingState()

    override fun execute(action: (Client) -> Unit) {
        throw OperationDeclinedException("Client is disconnecting")
    }
}