package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic

class LocalBroker(private val externalBroker: Broker) : Broker {

    private val subscriptions = mutableMapOf<Topic, SubscriptionList>()

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit): Broker.Subscription {
        synchronized(subscriptions) {
            val subscription = subscriptions[topic]
            if (subscription == null) {
                val sub = externalBroker.subscribe(topic) { t, d ->
                    synchronized(subscriptions) {
                        subscriptions[topic]?.propagate(t, d)
                    }
                }
                subscriptions[topic] = SubscriptionList(setOf(listener), sub)
            } else {
                subscriptions[topic] = subscription.copy(
                        listeners = subscription.listeners + setOf(listener)
                )
            }
        }
        return object : Broker.Subscription {
            override fun unsubscribe() {
                synchronized(subscriptions) {
                    val subscriptionList = subscriptions[topic]
                    val newListeners = subscriptionList!!.listeners - listener
                    if (newListeners.isEmpty()) {
                        subscriptions.remove(topic)
                        subscriptionList.externalSubscription.unsubscribe()
                    } else {
                        subscriptions[topic] = subscriptionList.copy(
                                listeners = newListeners
                        )
                    }
                }
            }
        }
    }

    override fun publish(topic: Topic, data: ByteArray): Broker.Publication {
        return externalBroker.publish(topic, data)
    }

    override fun addStateListener(listener: Broker.StateListener) {
        externalBroker.addStateListener(listener)
    }

    override fun removeStateListener(listener: Broker.StateListener) {
        externalBroker.removeStateListener(listener)
    }

    private data class SubscriptionList(
            val listeners: Set<(topic: Topic, data: ByteArray) -> Unit>,
            val externalSubscription: Broker.Subscription
    ) {
        fun propagate(topic: Topic, data: ByteArray) {
            listeners.forEach {
                it(topic, data)
            }
        }
    }
}