package com.seraph.smarthome.logic

import com.google.gson.Gson
import com.seraph.smarthome.model.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class DeviceManager(private val broker: Broker) {

    private val brokerQueue = Executors.newFixedThreadPool(1)
    private val devicesQueue = Executors.newFixedThreadPool(1)
    private val deviceCounter = AtomicInteger(0)

    fun addDevice(device: VirtualDevice) {
        devicesQueue.submit {
            val deviceIndex = deviceCounter.getAndIncrement()
            val id = Device.Id("device_$deviceIndex")
            val visitor = DiscoverVisitor(id)
            device.configure(visitor)
            val descriptor = visitor.formDescriptor("Device #$deviceIndex")
            brokerQueue.submit {
                broker.publish(Topics.structure(id), Gson().toJson(descriptor))
            }
            visitor.invalidateAll()
        }
    }

    private inner class DiscoverVisitor(private val deviceId: Device.Id) : VirtualDevice.Visitor {

        private val outputs = mutableListOf<Endpoint>()
        private val properties = mutableListOf<Property>()
        private val senders = mutableListOf<ValueSender<*>>()

        override fun declareBoolOutput(id: String, name: String):
                VirtualDevice.Updatable<Boolean> {

            val endpoint = Endpoint(
                    id = Endpoint.Id(id),
                    type = Endpoint.Type.BOOLEAN,
                    name = name
            )
            outputs.add(endpoint)

            val updater = ValueSender(
                    topic = Topics.output(deviceId, endpoint.id),
                    serializer = Boolean::toString
            )
            senders.add(updater)

            return updater
        }

        override fun declareIndicator(id: String, purpose: VirtualDevice.Purpose):
                VirtualDevice.Updatable<Boolean> {

            val property = Property(
                    id = Endpoint.Id(id),
                    type = Property.Type.INDICATOR,
                    purpose = mapFrom(purpose)
            )
            properties.add(property)

            val updater = ValueSender(
                    topic = Topics.property(deviceId, property.id),
                    serializer = Boolean::toString
            )
            senders.add(updater)

            return updater
        }

        override fun declareAction(id: String, purpose: VirtualDevice.Purpose):
                VirtualDevice.Observable<Unit> {

            val property = Property(
                    id = Endpoint.Id(id),
                    type = Property.Type.ACTION,
                    purpose = mapFrom(purpose)
            )
            properties.add(property)

            return ValueReceiver(
                    topic = Topics.property(deviceId, property.id),
                    deserializer = { Unit }
            )
        }

        fun formDescriptor(deviceName: String): Device = Device(
                deviceId, deviceName, emptyList(), outputs, properties
        )

        fun invalidateAll() {
            senders.forEach {
                it.invalidate()
            }
        }
    }

    private fun mapFrom(purpose: VirtualDevice.Purpose): Property.Purpose = when (purpose) {
        VirtualDevice.Purpose.STATE -> Property.Purpose.STATE
        VirtualDevice.Purpose.MAIN -> Property.Purpose.MAIN
        VirtualDevice.Purpose.PRIMARY -> Property.Purpose.PRIMARY
        VirtualDevice.Purpose.SECONDARY -> Property.Purpose.SECONDARY
    }

    inner class ValueReceiver<out T>(
            private val topic: Topic,
            private val deserializer: (String) -> T)
        : VirtualDevice.Observable<T> {

        override fun observe(observer: (T) -> Unit) {
            brokerQueue.submit {
                broker.subscribe(topic) { _, data ->
                    devicesQueue.submit {
                        observer.invoke(deserializer(data))
                    }
                }
            }
        }
    }

    inner class ValueSender<in T>(
            private val topic: Topic,
            private val serializer: (T) -> String)
        : VirtualDevice.Updatable<T> {

        private var source: (() -> T)? = null

        override fun use(source: () -> T) {
            this.source = source
        }

        override fun invalidate() {
            val source = this.source ?: return
            val value = source()
            brokerQueue.submit {
                broker.publish(topic, serializer(value))
            }
        }
    }
}