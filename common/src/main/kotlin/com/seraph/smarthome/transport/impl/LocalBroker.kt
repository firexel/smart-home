package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic

class LocalBroker(private val externalBroker: Broker) : Broker {

    private val subscriptions = mutableMapOf<Topic, Subscription>()

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit) {
        synchronized(subscriptions) {
            val subscription = subscriptions[topic]
            if (subscription == null) {
                subscriptions[topic] = Subscription(setOf(listener))
                externalBroker.subscribe(topic) { t, d ->
                    synchronized(subscriptions) {
                        subscriptions[topic]?.propagate(t, d)
                    }
                }
            } else {
                subscriptions[topic] = subscription.copy(
                        listeners = subscription.listeners + setOf(listener)
                )
            }
        }
    }

    override fun publish(topic: Topic, data: ByteArray): Broker.Publication {
        return externalBroker.publish(topic, data)
    }

    override fun addStateListener(listener: Broker.StateListener) {
        externalBroker.addStateListener(listener)
    }

    private data class Subscription(val listeners: Set<(topic: Topic, data: ByteArray) -> Unit>) {
        fun propagate(topic: Topic, data: ByteArray) {
            listeners.forEach {
                it(topic, data)
            }
        }
    }
}