package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.util.State

internal abstract class BaseState(protected val exchanger: Exchanger<BaseState, SharedData>) : State, Broker.BrokerState {
    protected fun transact(action: (SharedData) -> SharedData) {
        exchanger.transact { data ->
            if (data.state === this) {
                action(data)
            } else {
                data // without changes
            }
        }
    }

    protected fun sync(action: (SharedData) -> Unit) {
        exchanger.sync { data ->
            action(data)
        }
    }

    override fun toString(): String = this::class.simpleName.toString()

    abstract fun execute(key: Any? = null, action: (Client) -> Unit)
}