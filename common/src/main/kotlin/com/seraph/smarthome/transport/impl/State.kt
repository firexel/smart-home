package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker

internal interface State {
    fun engage()
    fun disengage()

    fun <T> accept(visitor: Broker.Visitor<T>): T
    fun execute(key: Any? = null, action: (Client) -> Unit)
}