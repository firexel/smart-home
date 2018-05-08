package com.seraph.smarthome.device

import com.seraph.smarthome.domain.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class DeviceManager(
        private val network: Network,
        private val rootId: Device.Id,
        private val brokerQueue: ExecutorService = Executors.newFixedThreadPool(1),
        private val devicesQueue: ExecutorService = Executors.newFixedThreadPool(1)
) {

    private val idCounters = mutableMapOf<String, Int>()

    fun addDriver(id: Device.Id, deviceDriver: DeviceDriver) {
        devicesQueue.run {
            val visitor = DiscoverVisitor(mergeRootAndDeviceIds(id))
            deviceDriver.configure(visitor)
            val descriptors = visitor.formDescriptors()
            brokerQueue.submit {
                descriptors.forEach { network.publish(it) }
            }
            visitor.invalidateAll()
        }
    }

    private fun mergeRootAndDeviceIds(id: Device.Id): Device.Id {
        return id.segments.dropLast(1)
                .fold(rootId) { acc, step -> acc.innerId(step) }
                .innerId("${id.segments.last()}_${nextIdIndex(id.toString())}")
    }

    private fun nextIdIndex(id: String): Int {
        idCounters.putIfAbsent(id, 0)
        val index = idCounters[id]!!
        idCounters[id] = index + 1
        return index
    }

    private inner class DiscoverVisitor(private val deviceId: Device.Id) : DeviceDriver.Visitor {

        private val endpoints = mutableListOf<Endpoint<*>>()
        private val controls = mutableListOf<Control>()
        private val outputs = mutableListOf<OutputImpl<*>>()
        private val innerDevices = mutableListOf<DiscoverVisitor>()

        override fun declareInnerDevice(id: String): DeviceDriver.Visitor {
            val innerVisitor = DiscoverVisitor(deviceId.innerId(id))
            innerDevices.add(innerVisitor)
            return innerVisitor
        }

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

        fun formDescriptors(): List<Device> {
            return listOf(Device(deviceId, endpoints, controls)) +
                    innerDevices.flatMap { it.formDescriptors() }

        }

        fun invalidateAll() {
            outputs.forEach { it.invalidate() }
            innerDevices.forEach { it.invalidateAll() }
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