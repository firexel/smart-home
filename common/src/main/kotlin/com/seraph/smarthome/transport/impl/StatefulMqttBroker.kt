package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import java.util.*

/**
 * Created by aleksandr.naumov on 02.01.18.
 */
class StatefulMqttBroker(
        address: String,
        name: String,
        private val log: Log
) : Broker {

    private val listeners = mutableListOf<Broker.StateListener>()
    private var exchanger: Exchanger<SharedData> = Exchanger(log.copy("Exchanger")) { state ->
        listeners.forEach { it.onStateChanged(state) }
    }

    init {
        val options = PahoClientWrapper.Options(
                hostUrl = address,
                name = name
        )
        exchanger.begin(SharedData(
                client = PahoClientWrapper(options, log.copy("Transport")),
                actions = LinkedList(),
                state = ConnectingState(exchanger),
                timesRetried = 0
        ))
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit)
            = exchanger.sync { data ->

        data.state.execute(topic) { client ->
            client.subscribe(topic) { topic, data ->
                listener(topic, data)
            }
            log.i("$topic subscribed")
        }
    }

    override fun publish(topic: Topic, data: ByteArray) = exchanger.sync {
        it.state.execute { client ->
            client.publish(topic, data)
        }
    }

    override fun addStateListener(listener: Broker.StateListener) = exchanger.sync {
        listeners.add(listener)
        listener.onStateChanged(it.state)
    }
}

