package com.seraph.smarthome.transport

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */
interface Broker {
    fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit)

    fun publish(topic: Topic, data: String, persisted: Boolean = true)

    fun <T> accept(visitor: Visitor<T>): T

    public interface Visitor<T> {
        fun onConnectedState(): T
        fun onDisconnectedState(): T
        fun onDisconnectingState(): T
        fun onWaitingState(msToReconnect: Long): T
        fun onConnectingState(): T
    }
}