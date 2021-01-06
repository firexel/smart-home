package com.seraph.smarthome.transport

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */
interface Broker {
    fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit): Subscription

    fun publish(topic: Topic, data: ByteArray): Publication

    fun addStateListener(listener: StateListener)

    fun removeStateListener(listener: StateListener)

    interface StateListener {
        fun onStateChanged(brokerState: BrokerState)
    }

    interface BrokerState {
        fun <T> accept(visitor: Visitor<T>): T

        interface Visitor<T> {
            fun onConnectedState(): T
            fun onDisconnectedState(): T
            fun onDisconnectingState(): T
            fun onWaitingState(msToReconnect: Long): T
            fun onConnectingState(): T
        }
    }

    interface Subscription {
        fun unsubscribe()
    }

    interface Publication {
        fun waitForCompletion(millis: Long)
    }
}