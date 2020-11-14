package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.util.Log
import java.lang.Long.max
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by aleksandr.naumov on 02.01.18.
 */
internal class StatefulMqttBroker(
        client: Client,
        private val log: Log
) : Broker {

    private val listeners = mutableListOf<Broker.StateListener>()
    private var exchanger: Exchanger<BaseState, SharedData> = Exchanger(log.copy("Exchanger")) { state ->
        listeners.forEach { it.onStateChanged(state) }
    }

    init {
        exchanger.begin(SharedData(
                client = client,
                actions = LinkedList(),
                state = ConnectingState(exchanger),
                timesRetried = 0
        ))
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit) = exchanger.sync { data ->
        data.state.execute(topic) { client ->
            client.subscribe(topic) { topic, data ->
                listener(topic, data)
            }
            log.i("Subscribed to $topic")
        }
        object : Broker.Subscription {
            override fun unsubscribe() {
                unsubscribe(topic)
            }
        }
    }

    fun unsubscribe(topic: Topic) = exchanger.sync { data ->
        data.state.execute(topic) { client ->
            client.unsubscribe(topic)
            log.i("Unsubscribed from $topic")
        }
    }

    override fun publish(topic: Topic, data: ByteArray): Broker.Publication = exchanger.sync {
        val publication = StatefulPublication()
        it.state.execute { client ->
            publication.setClientPublication(client.publish(topic, data))
        }
        publication
    }

    override fun addStateListener(listener: Broker.StateListener) = exchanger.sync {
        listeners.add(listener)
        listener.onStateChanged(it.state)
    }

    override fun removeStateListener(listener: Broker.StateListener) = exchanger.sync {
        listeners.remove(listener)
        Unit
    }

    private class StatefulPublication : Broker.Publication {
        private var latch: CountDownLatch? = null
        private var innerPublication: Client.Publication? = null

        fun setClientPublication(publication: Client.Publication) {
            synchronized(this) {
                innerPublication = publication
                latch?.countDown()
            }
        }

        override fun waitForCompletion(millis: Long) {
            var waitBudget = millis
            val latch: CountDownLatch? = synchronized(this) {
                if (innerPublication == null) {
                    CountDownLatch(1).apply { latch = this }
                } else {
                    null
                }
            }
            if (latch != null) {
                val beforeLatchWait = now()
                latch.await(millis, TimeUnit.MILLISECONDS)
                waitBudget = max(1, waitBudget - (now() - beforeLatchWait))
            }
            val publication = innerPublication
            if (publication == null) {
                throw TimeoutException("No publication was done in ${millis}ms")
            } else {
                publication.waitForCompletion(waitBudget)
            }
        }

        private fun now() = System.currentTimeMillis()
    }
}

