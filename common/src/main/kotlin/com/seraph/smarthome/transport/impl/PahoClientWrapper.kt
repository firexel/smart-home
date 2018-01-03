package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Topic
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
class PahoClientWrapper(private val options: Options) : Client {

    private val client: MqttAsyncClient

    init {
        client = MqttAsyncClient(options.hostUrl, options.name, MemoryPersistence())
    }

    override var disconnectionCallback: ((ClientException) -> Unit)? = null
        set(value) {
            field = value
            if (value == null) client.setCallback(null)
            else client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) = value(ClientException(cause))
                override fun messageArrived(topic: String?, message: MqttMessage?) = Unit
                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            })
        }

    override fun connect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit) = safe {
        client.connect(
                MqttConnectOptions().apply {
                    isAutomaticReconnect = options.autoReconnect
                    keepAliveInterval = options.keepAliveInterval
                },
                null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) = onSuccess()

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?)
                            = onFail(ClientException(exception))
                }
        )
    }

    override fun disconnect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit) = safe {
        client.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) = onSuccess()

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?)
                    = onFail(ClientException(exception))
        })
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit) = safe {
        client.subscribe(topic.toString(), options.subscribeQos) { topic, message ->
            listener(Topic.fromString(topic), String(message.payload, Charsets.UTF_8))
        }
    }

    override fun publish(topic: Topic, data: String) = safe {
        client.publish(topic.toString(), data.toByteArray(Charsets.UTF_8), options.publishQos, true)
    }

    private inline fun safe(operation: () -> Unit) {
        try {
            operation()
        } catch (ex: MqttException) {
            throw ClientException(ex)
        }
    }

    data class Options(
            val hostUrl: String,
            val name: String,
            val publishQos: Int = 1,
            val subscribeQos: Int = 1,
            val autoReconnect: Boolean = false,
            val keepAliveInterval: Int = 10
    )
}