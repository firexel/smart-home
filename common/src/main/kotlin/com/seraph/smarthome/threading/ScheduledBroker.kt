package com.seraph.smarthome.threading

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import java.util.concurrent.ConcurrentHashMap

class ScheduledBroker(
        private val wrapped: Broker,
        private val executor: Executor,
) : Broker {

    private val associatedListeners = ConcurrentHashMap<Broker.StateListener, Broker.StateListener>()

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit):
            Broker.Subscription {

        return wrapped.subscribe(topic) { t, d ->
            executor.run {
                listener(t, d)
            }
        }
    }

    override fun publish(topic: Topic, data: ByteArray): Broker.Publication {
        return wrapped.publish(topic, data)
    }

    override fun addStateListener(listener: Broker.StateListener) {
        val wrapListener = object : Broker.StateListener {
            override fun onStateChanged(brokerState: Broker.BrokerState) {
                executor.run {
                    listener.onStateChanged(brokerState)
                }
            }
        }
        associatedListeners[listener] = wrapListener
        wrapped.addStateListener(wrapListener)
    }

    override fun removeStateListener(listener: Broker.StateListener) {
        associatedListeners.remove(listener)?.let {
            wrapped.removeStateListener(it)
        }
    }
}