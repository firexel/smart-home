package com.seraph.smarthome.util

import java.util.*

class Exchanger<S : State, D : Exchanger.StateData<S>>(
        private val log: Log = NoLog(),
        private var listener: (S) -> Unit = {}
) {

    private lateinit var sharedData: D
    private val currentTransactions = LinkedList<(D) -> D>()

    fun begin(initialData: D) {
        sharedData = initialData
        synchronized(this) {
            val state = initialData.state
            log.i("--> $state")
            state.engage()
            listener(state)
        }
    }

    fun transact(transaction: (D) -> D) {
        synchronized(this) {
            val transactions = currentTransactions
            transactions.add(transaction)
            if (transactions.size == 1) {
                var data = sharedData
                var oldState = data.state
                while (transactions.isNotEmpty()) {
                    val transact = transactions.first
                    data = transact(data)
                    val newState = data.state
                    if (oldState !== newState) {
                        log.i("$oldState --> $newState")
                        oldState.disengage()
                        newState.engage()
                        oldState = newState
                        listener(newState)
                    }
                    transactions.removeFirst()
                }
                sharedData = data
            }
            Unit
        }
    }

    fun <R> sync(extractor: (D) -> R): R {
        synchronized(this) {
            return extractor(sharedData)
        }
    }

    interface StateData<S : State> {
        val state: S
    }
}