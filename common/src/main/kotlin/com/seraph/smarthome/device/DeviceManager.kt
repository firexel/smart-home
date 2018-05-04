package com.seraph.smarthome.device

import com.seraph.smarthome.domain.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class DeviceManager(private val network: Network) {

    private val brokerQueue = Executors.newFixedThreadPool(1)
    private val devicesQueue = Executors.newFixedThreadPool(1)
    private val deviceCounter = AtomicInteger(0)

    fun addDriver(deviceDriver: DeviceDriver) {
        devicesQueue.run {
            val deviceIndex = deviceCounter.getAndIncrement()
            val id = Device.Id(listOf("logic", "$deviceIndex"))
            val visitor = DiscoverVisitor(id)
            deviceDriver.configure(visitor)
            val descriptor = visitor.formDescriptor()
            brokerQueue.submit {
                network.publish(descriptor)
            }
            visitor.invalidateAll()
        }
    }

    private inner class DiscoverVisitor(private val deviceId: Device.Id) : DeviceDriver.Visitor {

        private val endpoints = mutableListOf<Endpoint<*>>()
        private val controls = mutableListOf<Control>()
        private val outputs = mutableListOf<OutputImpl<*>>()

        override fun <T> declareInput(id: String, type: Endpoint.Type<T>, retention: Endpoint.Retention):
                DeviceDriver.Input<T> {

            val endpoint = Endpoint(
                    Endpoint.Id(id),
                    type,
                    Endpoint.Direction.INPUT,
                    retention
            )

            endpoints.add(endpoint)
            return InputImpl(deviceId, endpoint)
        }

        override fun <T> declareOutput(id: String, type: Endpoint.Type<T>, retention: Endpoint.Retention):
                DeviceDriver.Output<T> {

            val endpoint = Endpoint(
                    Endpoint.Id(id),
                    type,
                    Endpoint.Direction.OUTPUT,
                    retention
            )

            endpoints.add(endpoint)
            return OutputImpl(deviceId, endpoint)
        }

        override fun declareIndicator(id: String, priority: Control.Priority, source: DeviceDriver.Output<Boolean>) {
            controls.add(Control(
                    Control.Id(id),
                    priority,
                    Indicator((source as OutputImpl<Boolean>).endpoint)
            ))
        }

        override fun declareButton(id: String, priority: Control.Priority, input: DeviceDriver.Input<Unit>, alert: String) {
            controls.add(Control(
                    Control.Id(id),
                    priority,
                    Button((input as InputImpl<Unit>).endpoint, alert)
            ))
        }

        fun formDescriptor(): Device = Device(
                deviceId, endpoints, controls
        )

        fun invalidateAll() {
            outputs.forEach {
                it.invalidate()
            }
        }
    }

    inner class OutputImpl<T>(
            private val deviceId: Device.Id,
            val endpoint: Endpoint<T>
    ) : DeviceDriver.Output<T> {

        private var source: () -> T = { throw IllegalStateException("Source should be set") }

        override fun use(source: () -> T) {
            this.source = source
        }

        override fun invalidate() {
            val data = source()
            brokerQueue.run {
                network.publish(deviceId, endpoint, data)
            }
        }
    }

    inner class InputImpl<T>(
            private val deviceId: Device.Id,
            val endpoint: Endpoint<T>
    ) : DeviceDriver.Input<T> {

        override fun observe(observer: (T) -> Unit) {
            brokerQueue.run {
                network.subscribe(deviceId, endpoint) { _, _, data ->
                    devicesQueue.run {
                        observer(data)
                    }
                }
            }
        }
    }
}