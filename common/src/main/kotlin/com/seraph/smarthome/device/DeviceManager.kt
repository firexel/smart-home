package com.seraph.smarthome.device

import com.seraph.smarthome.domain.*
import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class DeviceManager(
        private val network: Network,
        private val rootId: Device.Id,
        private val brokerQueue: Executor = Executors.newFixedThreadPool(1),
        private val log: Log = NoLog()
) {

    private val idCounters = mutableMapOf<String, Int>()

    fun addDriver(
            id: Device.Id,
            deviceDriver: DeviceDriver,
            executor: Executor = Executors.newFixedThreadPool(1)) {

        val context = DeviceContext(id = mergeRootAndDeviceIds(id), executor = executor)
        context.run {
            DiscoverVisitor(context).configure(deviceDriver)
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

    private inner class DiscoverVisitor(
            private var context: DeviceContext
    ) : DeviceDriver.Visitor {

        override fun declareOutputPolicy(policy: DeviceDriver.OutputPolicy) {
            context = context.changePolicy(when (policy) {
                DeviceDriver.OutputPolicy.WAIT_FOR_ALL_INPUTS -> WaitForAllInputsPolicy()
                DeviceDriver.OutputPolicy.ALWAYS_ALLOW -> AlwaysAllowPolicy()
            })
        }

        private val endpoints = mutableListOf<Endpoint<*>>()
        private val controls = mutableListOf<Control>()
        private val retainedOutputs = mutableListOf<RetainedOutput<*>>()
        private val innerDevices = mutableListOf<DiscoverVisitor>()

        override fun declareInnerDevice(id: String): DeviceDriver.Visitor {
            val innerVisitor = DiscoverVisitor(context.inner(id))
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

            context.notifyInputExits(endpoint.id)
            endpoints.add(endpoint)
            return InputImpl(context, endpoint)
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
            return createOutput(endpoint, retention)
        }

        private fun <T> createOutput(endpoint: Endpoint<T>, retention: Endpoint.Retention): DeviceDriver.Output<T> {
            return when (retention) {
                Endpoint.Retention.RETAINED -> {
                    RetainedOutput(context, endpoint).apply { retainedOutputs.add(this) }
                }
                Endpoint.Retention.NOT_RETAINED -> {
                    NonRetainedOutput(context, endpoint)
                }
            }
        }

        override fun declareIndicator(id: String, priority: Control.Priority, source: DeviceDriver.Output<Boolean>) {
            controls.add(Control(
                    Control.Id(id),
                    priority,
                    Indicator((source as EndpointContainer<Boolean>).endpoint)
            ))
        }

        override fun declareButton(id: String, priority: Control.Priority, input: DeviceDriver.Input<Unit>, alert: String) {
            controls.add(Control(
                    Control.Id(id),
                    priority,
                    Button((input as EndpointContainer<Unit>).endpoint, alert)
            ))
        }

        fun formDescriptors(): List<Device> {
            return listOf(Device(context.id, endpoints, controls)) +
                    innerDevices.flatMap { it.formDescriptors() }

        }

        fun configure(driver: DeviceDriver) {
            driver.configure(this)
            val descriptors = formDescriptors()
            brokerQueue.execute {
                descriptors.forEach {
                    network.publish(it)
                            .waitForCompletion(1000)
                }
            }
            invalidateAll()
        }

        fun invalidateAll() {
            retainedOutputs.forEach { it.publishSetValue() }
            innerDevices.forEach { it.invalidateAll() }
        }
    }

    private inner class NonRetainedOutput<T>(
            private val context: DeviceContext,
            override val endpoint: Endpoint<T>
    ) : DeviceDriver.Output<T>, EndpointContainer<T> {

        override fun set(update: T) {
            context.run {
                if (!context.outputLocked) {
                    brokerQueue.execute {
                        network.publish(context.id, endpoint, update)
                                .waitForCompletion(1000)
                    }
                }
            }
        }
    }

    private inner class RetainedOutput<T>(
            private val context: DeviceContext,
            override val endpoint: Endpoint<T>
    ) : DeviceDriver.Output<T>, EndpointContainer<T> {

        private var valueIsSet = false

        private val isLocked
            get() = context.outputLocked

        private var retainedValue: T? = null

        override fun set(update: T) {
            context.run {
                if (!isLocked && !postingSameAsRetained(update)) {
                    retainValue(update)
                    fireUpdate(update)
                }
            }
        }

        private fun fireUpdate(update: T) {
            brokerQueue.execute {
                network.publish(context.id, endpoint, update)
                        .waitForCompletion(1000)
            }
        }

        private fun retainValue(update: T) {
            valueIsSet = true
            retainedValue = update
        }

        private fun postingSameAsRetained(update: T) = valueIsSet && update == retainedValue

        fun publishSetValue() {
            val value = retainedValue
            if (valueIsSet && !isLocked && value != null) {
                fireUpdate(value)
            }
        }
    }

    private interface EndpointContainer<T> {
        val endpoint: Endpoint<T>
    }

    inner class InputImpl<T>(
            private val context: DeviceContext,
            override val endpoint: Endpoint<T>
    ) : DeviceDriver.Input<T>, EndpointContainer<T> {

        override fun observe(observer: (T) -> Unit) {
            brokerQueue.execute {
                network.subscribe(context.id, endpoint) { _, _, data ->
                    context.run {
                        context.notifyInputGained(endpoint.id)
                        observer(data)
                    }
                }
            }
        }
    }

    data class DeviceContext(
            val executor: Executor,
            private val outputPolicy: OutputPolicy = AlwaysAllowPolicy(),
            val id: Device.Id
    ) {
        inline fun <R> run(block: () -> R): R {
            return executor.run { block() }
        }

        fun inner(newId: String): DeviceContext = copy(id = id.innerId(newId))

        fun changePolicy(policy: OutputPolicy) = copy(outputPolicy = policy)

        val outputLocked: Boolean
            get() = outputPolicy.locked

        fun notifyInputExits(endpoint: Endpoint.Id) {
            outputPolicy.lock(endpoint)
        }

        fun notifyInputGained(endpoint: Endpoint.Id) {
            outputPolicy.unlock(endpoint)
        }
    }

    interface OutputPolicy {
        val locked: Boolean
        fun lock(endpoint: Endpoint.Id)
        fun unlock(endpoint: Endpoint.Id)
    }

    class AlwaysAllowPolicy : OutputPolicy {
        override val locked: Boolean = false

        override fun lock(endpoint: Endpoint.Id) {
        }

        override fun unlock(endpoint: Endpoint.Id) {
        }
    }

    class WaitForAllInputsPolicy : OutputPolicy {

        private val locks = mutableSetOf<Endpoint.Id>()

        override val locked: Boolean
            get() = locks.isNotEmpty()

        override fun lock(endpoint: Endpoint.Id) {
            locks.add(endpoint)
        }

        override fun unlock(endpoint: Endpoint.Id) {
            locks.remove(endpoint)
        }
    }
}