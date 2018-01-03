package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*

/**
 * Created by aleksandr.naumov on 02.01.18.
 */
public class StatefullMqttBroker(
        private val log: Log,
        address: String,
        name: String
) : Broker {

    private var exchanger: Exchanger = Exchanger()

    init {
        exchanger.begin(SharedData(
                client = MqttAsyncClient(address, name, MemoryPersistence()),
                actions = LinkedList(),
                state = ConnectingState(exchanger),
                timesRetried = 0
        ))
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit)
            = exchanger.sync { data ->

        data.state.execute { client ->
            client.subscribe(topic.toString(), 1, Listener(listener))
            log.i("$topic subscribed")
        }
    }

    override fun publish(topic: Topic, data: String) = exchanger.sync {
        it.state.execute { client ->
            client.publish(topic.toString(), data.toByteArray(), 1, true)
            log.i("$topic <- $data")
        }
    }

    override fun <T> accept(visitor: Broker.Visitor<T>): T = exchanger.sync {
        it.state.accept(visitor)
    }

    inner class Listener(private val listener: (topic: Topic, data: String) -> Unit)
        : IMqttMessageListener {

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            val data = String(message!!.payload)
            log.i("$topic -> $data")
            listener(Topic.fromString(topic!!), data)
        }
    }
}

