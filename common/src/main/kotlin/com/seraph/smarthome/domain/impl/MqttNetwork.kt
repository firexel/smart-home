package com.seraph.smarthome.domain.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.seraph.smarthome.domain.*
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log

class MqttNetwork(
        private val transport: Broker,
        private val log: Log
) : Network {

    private val gson: Gson = with(GsonBuilder()) {
        installModelAdapters(this)
        create()
    }

    override fun publish(metainfo: Metainfo): Network.Publication =
            publish(Topics.metadata(), JsonSerializer(gson, Metainfo::class), metainfo)

    override fun subscribe(func: (Metainfo) -> Unit) {
        subscribe(Topics.metadata(), JsonSerializer(gson, Metainfo::class), func)
    }

    override fun publish(device: Device): Network.Publication =
            publish(Topics.structure(device.id), JsonSerializer(gson, Device::class), device)

    override fun subscribe(device: Device.Id?, func: (Device) -> Unit) {
        subscribe(Topics.structure(device), JsonSerializer(gson, Device::class), func)
    }

    override fun <T> publish(device: Device.Id, endpoint: Endpoint<T>, data: T): Network.Publication =
            publish(Topics.endpoint(device, endpoint), endpoint.type.serializer, data)


    override fun <T> subscribe(
            device: Device.Id,
            endpoint: Endpoint<T>,
            func: (Device.Id, Endpoint<T>, data: T) -> Unit) {

        subscribe(Topics.endpoint(device, endpoint), endpoint.type.serializer) { data ->
            func(device, endpoint, data)
        }
    }

    private fun <T> subscribe(topic: Topic, serializer: Serializer<T>, acceptor: (T) -> Unit) {
        transport.subscribe(topic) { responseTopic, data ->
            try {
                val deserialized = serializer.fromBytes(data)
                log.v("$responseTopic --> $deserialized")
                acceptor(deserialized)
            } catch (ex: Serializer.TypeMismatchException) {
                log.w("Type mismatch during parsing of $topic")
            }
        }
    }

    private fun <T> publish(topic: Topic, serializer: Serializer<T>, data: T): NetworkPublication {
        log.v("$topic <-- $data")
        return NetworkPublication(transport.publish(topic, serializer.toBytes(data)))
    }

    override var statusListener: Network.StatusListener = NoListener()

    private class NetworkPublication(private val publication: Broker.Publication) : Network.Publication {
        override fun waitForCompletion(millis: Long) {
            publication.waitForCompletion(millis)
        }
    }

    class NoListener : Network.StatusListener {
        override fun onStatusChanged(status: Network.Status) {
            // do nothing
        }
    }
}