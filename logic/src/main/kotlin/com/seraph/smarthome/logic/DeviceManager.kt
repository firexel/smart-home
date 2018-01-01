package com.seraph.smarthome.logic

import com.seraph.smarthome.model.Device
import com.seraph.smarthome.model.Endpoint
import com.seraph.smarthome.model.Property
import com.seraph.smarthome.transport.*
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
            val descriptor = visitor.formDescriptor("Io Device #$deviceIndex")
            brokerQueue.submit {
                Topics.structure(id).publish(broker, descriptor)
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
                    Topics.output(deviceId, endpoint.id).typed(BooleanConverter())
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
                    Topics.property(deviceId, property.id).typed(BooleanConverter())
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

            return ValueReceiver(Topics.property(deviceId, property.id).typed(ActionConverter()))
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
        VirtualDevice.Purpose.MAIN -> Property.Purpose.MAIN
        VirtualDevice.Purpose.PRIMARY -> Property.Purpose.PRIMARY
        VirtualDevice.Purpose.SECONDARY -> Property.Purpose.SECONDARY
    }

    inner class ValueReceiver<out T>(
            private val topic: TypedTopic<T>)
        : VirtualDevice.Observable<T> {

        override fun observe(observer: (T) -> Unit) {
            brokerQueue.submit {
                topic.subscribe(broker) { data ->
                    devicesQueue.submit {
                        observer(data)
                    }
                }
            }
        }
    }

    inner class ValueSender<in T>(
            private val topic: TypedTopic<T>)
        : VirtualDevice.Updatable<T> {

        private var source: (() -> T)? = null

        override fun use(source: () -> T) {
            this.source = source
        }

        override fun invalidate() {
            val source = this.source ?: return
            val value = source()
            brokerQueue.submit {
                topic.publish(broker, value)
            }
        }
    }
}