package com.seraph.smarthome.transport.impl

internal class Exchanger<D : Exchanger.StateData> {

    private lateinit var sharedData: D

    fun begin(initialData: D) {
        sharedData = initialData
        synchronized(this) {
            initialData.state.engage()
        }
    }

    fun transact(action: (D) -> D) {
        synchronized(this) {
            val oldData = sharedData
            val oldState = oldData.state
            val newData = action(oldData)
            val newState = newData.state
            sharedData = newData
            if (oldState !== newState) {
                oldState.disengage()
                newState.engage()
            }
        }
    }

    fun <R> sync(extractor: (D) -> R): R {
        synchronized(this) {
            return extractor(sharedData)
        }
    }

    interface StateData {
        val state: State
    }
}