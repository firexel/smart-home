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

    override fun connect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit): Unit = safe("connect") {
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

    override fun disconnect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit): Unit = safe("disconnect") {
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

    override fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit): Unit = safe("subscribe") {
        client.subscribe(topic.toString(), options.subscribeQos) { topic, message ->
            listener(Topic.fromString(topic), message.payload)
        }
    }

    override fun publish(topic: Topic, data: ByteArray): Client.Publication = safe("publish") {
        return MqttPublication(client.publish(topic.toString(), data, options.publishQos, topic.persisted))
    }

    private inline fun <T> safe(name: String, operation: () -> T): T {
        try {
            return operation()
        } catch (ex: MqttException) {
            throwClientException(ex, name)
        } catch (ex: IOException) {
            throwClientException(ex, name)
        }
    }

    private fun throwClientException(ex: Throwable, name: String): Nothing {
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

    private class MqttPublication(val token: IMqttDeliveryToken) : Client.Publication {
        override fun waitForCompletion(millis: Long) {
            token.waitForCompletion(millis)
        }
    }
}