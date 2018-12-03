package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.transport.Broker

internal class DisconnectingState(exchanger: Exchanger<BaseState, SharedData>) : BaseState(exchanger) {
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

    override fun <T> accept(visitor: Broker.BrokerState.Visitor<T>): T = visitor.onDisconnectingState()

    override fun execute(key: Any?, action: (Client) -> Unit) {
        throw OperationDeclinedException("Client is disconnecting")
    }
}