package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.util.Log
import java.util.*

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
        var publication: StatefulPublication? = null
        it.state.execute { client ->
            publication = StatefulPublication(client.publish(topic, data))
        }
        publication!!
    }

    override fun addStateListener(listener: Broker.StateListener) = exchanger.sync {
        listeners.add(listener)
        listener.onStateChanged(it.state)
    }

    override fun removeStateListener(listener: Broker.StateListener) = exchanger.sync {
        listeners.remove(listener)
        Unit
    }

    private class StatefulPublication(private val publication: Client.Publication) : Broker.Publication {
        override fun waitForCompletion(millis: Long) {
            publication.waitForCompletion(millis)
        }
    }
}

