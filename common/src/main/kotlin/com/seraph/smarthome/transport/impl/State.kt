package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker

internal interface State : Broker.BrokerState {
    fun engage()
    fun disengage()

    fun execute(key: Any? = null, action: (Client) -> Unit)
}