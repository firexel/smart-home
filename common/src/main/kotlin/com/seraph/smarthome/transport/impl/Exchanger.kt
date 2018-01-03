package com.seraph.smarthome.transport.impl

class Exchanger {

    private lateinit var sharedData: SharedData

    fun begin(initialData: SharedData) {
        sharedData = initialData
        synchronized(this) {
            initialData.state.engage()
        }
    }

    fun transact(action: (SharedData) -> SharedData) {
        synchronized(this) {
            val oldData = sharedData
            val oldState = oldData.state
            val newData = action(oldData)
            val newState = newData.state
            sharedData = newData
            if (oldState !== newState) {
                oldState.disengage(newData)
                newState.engage()
            }
        }
    }

    fun <T> sync(extractor: (SharedData) -> T): T {
        synchronized(this) {
            return extractor(sharedData)
        }
    }
}