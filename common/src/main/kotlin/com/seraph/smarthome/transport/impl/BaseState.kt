package com.seraph.smarthome.transport.impl

abstract class BaseState(protected val exchanger: Exchanger<SharedData>) : State {
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
}