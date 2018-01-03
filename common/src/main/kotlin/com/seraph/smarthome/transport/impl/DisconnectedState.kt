package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker

class DisconnectedState(exchanger: Exchanger<SharedData>) : BaseState(exchanger) {
    override fun engage() = Unit

    override fun disengage() = Unit

    override fun <T> accept(visitor: Broker.Visitor<T>): T = visitor.onDisconnectedState()

    override fun execute(action: (Client) -> Unit) {
        throw OperationDeclinedException("Client is disconnected")
    }
}