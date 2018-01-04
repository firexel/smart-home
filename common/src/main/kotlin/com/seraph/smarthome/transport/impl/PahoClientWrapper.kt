package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal class PahoClientWrapper(
        private val options: Options,
        private val log: Log
) : Client {

    private val client: MqttAsyncClient

    init {
        client = MqttAsyncClient(options.hostUrl, options.name, MemoryPersistence())
    }

    override var disconnectionCallback: ((ClientException) -> Unit)? = null
        set(value) {
            field = value
            if (value == null) client.setCallback(null)
            else client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) = value(PahoClientException(cause))
                override fun messageArrived(topic: String?, message: MqttMessage?) = Unit
                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            })
        }

    override fun connect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit) = safe("connect") {
        client.connect(
                MqttConnectOptions().apply {
                    isAutomaticReconnect = options.autoReconnect
                    keepAliveInterval = options.keepAliveInterval
                    isCleanSession = true
                },
                null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        log.i("Connected")
                        onSuccess()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?)
                            = with(PahoClientException(exception)) {
                        log.w("Connection failed because of $message")
                        onFail(this)
                    }
                }
        )
    }

    override fun disconnect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit) = safe("disconnect") {
        client.disconnect(null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                log.i("Disconnected")
                onSuccess()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?)
                    = with(PahoClientException(exception)) {
                log.w("Connection failed because of $message")
                onFail(this)
            }
        })
    }

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: String) -> Unit) = safe("subscribe") {
        client.subscribe(topic.toString(), options.subscribeQos) { topic, message ->
            listener(Topic.fromString(topic), String(message.payload, Charsets.UTF_8))
        }
    }

    override fun publish(topic: Topic, data: String) = safe("publish") {
        client.publish(topic.toString(), data.toByteArray(Charsets.UTF_8), options.publishQos, topic.persisted)
    }

    private inline fun safe(name: String, operation: () -> Unit) {
        try {
            operation()
        } catch (ex: MqttException) {
            throwClientException(ex, name)
        } catch (ex: IOException) {
            throwClientException(ex, name)
        }
    }

    private fun throwClientException(ex: Throwable, name: String) {
        with(PahoClientException(ex)) {
            log.w("Error occurred during $name: $message")
            throw this
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