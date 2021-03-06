package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.transport.Broker

internal class DisconnectedState(exchanger: Exchanger<BaseState, SharedData>) : BaseState(exchanger) {
    override fun engage() = transact { it.copy(actions = emptyList()) }

    override fun disengage() = Unit

    override fun <T> accept(visitor: Broker.BrokerState.Visitor<T>): T = visitor.onDisconnectedState()

    override fun execute(key:Any?, action: (Client) -> Unit) {
        throw OperationDeclinedException("Client is disconnected")
    }
}