package com.seraph.smarthome.model

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */
interface Broker {
    fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit)
    fun publish(topic: Topic, data: String)
}

class MqttBroker(address: String, name: String, private val log: Log) : Broker {

    val client: MqttAsyncClient = MqttAsyncClient(address, name, MemoryPersistence())

    init {
        val latch = CountDownLatch(1)
        var error: Throwable? = null
        client.connect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                latch.countDown()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                error = exception ?: IOException("Cannot connect to broker at $address")
                latch.countDown()
            }
        })
        if (!latch.await(30, TimeUnit.SECONDS) || error != null) {
            throw error ?: IOException("Connection timed out at $address")
        }
        log.i("Connected")
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit) {
        client.subscribe(topic.toString(), 0, Listener(listener))
        log.i("$topic subscribed")
    }


    override fun publish(topic: Topic, data: String) {
        client.publish(topic.toString(), data.toByteArray(), 0, true)
        log.i("$topic <- $data")
    }

    inner class Listener(private val listener: (topic: Topic, data: String) -> Unit) : IMqttMessageListener {
        override fun messageArrived(topic: String?, message: MqttMessage?) {
            val data = String(message!!.payload)
            log.i("$topic -> $data")
            listener(Topic.fromString(topic!!), data)
        }
    }
}