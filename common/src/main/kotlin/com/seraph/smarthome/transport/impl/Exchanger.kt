package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog
import java.util.*

internal class Exchanger<D : Exchanger.StateData>(private val log: Log = NoLog()) {

    private lateinit var sharedData: D
    private val currentTransactions = LinkedList<(D) -> D>()

    fun begin(initialData: D) {
        sharedData = initialData
        synchronized(this) {
            val state = initialData.state
            log.i("--> $state")
            state.engage()
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

    interface StateData {
        val state: State
    }
}