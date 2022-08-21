package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.threading.Executor
import com.seraph.smarthome.threading.ThreadExecutor
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog

class WildcardBroker(
    private val wrapped: Broker,
    private val executor: Executor = ThreadExecutor(),
    private val log: Log = NoLog(),
    private val wildcards: List<Topic> = emptyList()
) : Broker {

    private val subs: MutableList<Subscription> = mutableListOf()

    data class Listener(
        val topic: Topic,
        val callback: (Topic, ByteArray) -> Unit
    )

    private inner class Subscription(val wildcard: Topic) {
        private val listeners: MutableMap<Any, Listener> = mutableMapOf()
        private var subscription: Broker.Subscription? = null
        private val values: MutableMap<Topic, ByteArray> = mutableMapOf()
        private val lock = Any()

        fun addListener(topic: Topic, listener: (Topic, ByteArray) -> Unit): Broker.Subscription {
            return synchronized(lock) {
                val token = Any()
                listeners[token] = Listener(topic, listener)
                if (listeners.size == 1) {
                    log.v("-*> '$topic' > '$wildcard'")
                    subscription = wrapped.subscribe(wildcard) { t, d ->
                        store(t, d)
                        propagate(t, d)
                    }
                }
                feedRetainedValues(topic, listener)
                object : Broker.Subscription {
                    override fun unsubscribe() {
                        synchronized(lock) {
                            listeners.remove(token)
//                            TODO("Fix unsubscribe")
//                            if (listeners.isEmpty()) {
//                                subscription?.unsubscribe()
//                                values.clear()
//                            }
                        }
                    }
                }
            }
        }

        private fun store(t: Topic, d: ByteArray) {
            if (t.persisted) {
                synchronized(lock) {
                    values[t] = d
                }
            }
        }

        private fun feedRetainedValues(topic: Topic, listener: (Topic, ByteArray) -> Unit) {
            synchronized(lock) {
                values.filter { it.key.matches(topic) }.forEach {
                    executor.run { listener(it.key, it.value) }
                }
            }
        }

        private fun propagate(topic: Topic, bytes: ByteArray) {
            synchronized(lock) {
                listeners.values
                    .filter { it.topic.matches(topic) }
                    .forEach { executor.run { it.callback(topic, bytes) } }
            }
        }
    }

    override fun subscribe(
        topic: Topic,
        listener: (topic: Topic, data: ByteArray) -> Unit
    ): Broker.Subscription {
        return synchronized(this) {
            var activeSub = subs.find { it.wildcard.matches(topic) }
            if (activeSub == null) {
                activeSub = Subscription(wildcards.find { it.matches(topic) } ?: topic)
                subs.add(activeSub)
            }
            activeSub.addListener(topic, listener)
        }
    }

    override fun publish(topic: Topic, data: ByteArray): Broker.Publication {
        return wrapped.publish(topic, data)
    }

    override fun addStateListener(listener: Broker.StateListener) {
        wrapped.addStateListener(listener)
    }

    override fun removeStateListener(listener: Broker.StateListener) {
        wrapped.removeStateListener(listener)
    }

    private fun Topic.matches(topic: Topic): Boolean {
        if (segments.size != topic.segments.size) {
            return false
        }
        segments.forEachIndexed { i, segmentA ->
            val segmentB = topic.segments[i]
            if (segmentA != segmentB && segmentA != "+" && segmentB != "+") {
                return false
            }
        }
        return true
    }
}