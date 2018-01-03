package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import java.util.*

/**
 * Created by aleksandr.naumov on 02.01.18.
 */
class StatefullMqttBroker(
        private val log: Log,
        address: String,
        name: String
) : Broker {

    private var exchanger: Exchanger<SharedData> = Exchanger()

    init {
        val options = PahoClientWrapper.Options(
                hostUrl = address,
                name = name
        )
        exchanger.begin(SharedData(
                client = PahoClientWrapper(options),
                actions = LinkedList(),
                state = ConnectingState(exchanger),
                timesRetried = 0
        ))
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit)
            = exchanger.sync { data ->

        data.state.execute { client ->
            client.subscribe(topic) { topic, data ->
                log.i("$topic -> $data")
                listener(topic, data)
            }
            log.i("$topic subscribed")
        }
    }

    override fun publish(topic: Topic, data: String) = exchanger.sync {
        it.state.execute { client ->
            client.publish(topic, data)
            log.i("$topic <- $data")
        }
    }

    override fun <T> accept(visitor: Broker.Visitor<T>): T = exchanger.sync {
        it.state.accept(visitor)
    }
}

