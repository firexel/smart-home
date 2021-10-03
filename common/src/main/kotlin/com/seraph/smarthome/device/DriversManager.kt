package com.seraph.smarthome.device

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.domain.Units
import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class DriversManager(
        private val network: Network,
        private val rootId: Device.Id,
        private val brokerQueue: Executor = Executors.newFixedThreadPool(1),
        private val log: Log = NoLog()
) {

    fun addDriver(
            id: Device.Id,
            driver: DeviceDriver,
            executor: Executor = Executors.newFixedThreadPool(1)) {

        val context = DeviceThreadContext(id = mergeRootAndDeviceIds(id), executor = executor)
        context.run {
            DiscoverVisitor(context, log).bind(driver)
        }
    }

    private fun mergeRootAndDeviceIds(id: Device.Id): Device.Id {
        return Device.Id(rootId.segments + id.segments)
    }

    private inner class DiscoverVisitor(
            private var context: DeviceThreadContext,
            private var log: Log
    ) : DeviceDriver.Visitor {

        private val inputs = mutableListOf<InputImpl<*>>()
        private val outputs = mutableListOf<OutputImpl<*>>()
        private val innerDevices = mutableListOf<DiscoverVisitor>()
        private var operationalCallback: () -> Unit = {}

        override fun declareInnerDevice(id: String): DeviceDriver.Visitor {
            val innerVisitor = DiscoverVisitor(context.inner(id), log.copy(id))
            innerDevices.add(innerVisitor)
            return innerVisitor
        }

        override fun <T> declareInput(id: String, type: Endpoint.Type<T>):
                DeviceDriver.Input<T> {

            return InputImpl(context, Endpoint.Id(id), type, log.copy(id)).apply { inputs.add(this) }
        }

        override fun <T> declareOutput(id: String, type: Endpoint.Type<T>):
                DeviceDriver.Output<T> {

            return OutputImpl(context, Endpoint.Id(id), type, log.copy(id)).apply { outputs.add(this) }
        }

        override fun onOperational(operation: () -> Unit) {
            operationalCallback = operation
        }

        fun finishBuilding(): List<Device> {
            val endpoints =
                    outputs.map { it.onFinishBuilding() } + inputs.map { it.onFinishBuilding() }

            return listOf(Device(context.id, endpoints)) +
                    innerDevices.flatMap { it.finishBuilding() }
        }

        private fun publishDeviceDescriptors(descriptors: List<Device>) {
            brokerQueue.execute {
                descriptors.forEach {
                    log.v("Publishing descriptor for ${it.id}")
                    network.publish(it).waitForCompletion(1000)
                }
            }
            log.v("All descriptors enqueued for publish")
        }

        fun startInteraction() {
            inputs.forEach { it.onStartInteraction() }
            context.setOperationalCallback {
                outputs.forEach { it.onStartInteraction() }
                log.i("Now operational")
                operationalCallback()
                operationalCallback = {}
            }
            innerDevices.forEach { it.startInteraction() }
        }

        fun bind(driver: DeviceDriver) {
            driver.bind(this)
            val descriptors = finishBuilding()
            publishDeviceDescriptors(descriptors)
            startInteraction()
        }
    }

    private inner class OutputImpl<T>(
            private val context: DeviceThreadContext,
            id: Endpoint.Id,
            type: Endpoint.Type<T>,
            private val log: Log
    ) : DeviceDriver.Output<T> {

        private var builder: EndpointBuilder<T>? = EndpointBuilder(id, type, Endpoint.Direction.OUTPUT)
        private var runtime: OutputRuntime<T>? = null

        private fun getBuilder(): EndpointBuilder<T> {
            return builder ?: throw IllegalStateException("Already finalized")
        }

        private fun getRuntime(): OutputRuntime<T> {
            return runtime ?: throw IllegalStateException("Not yet finalized")
        }

        override fun setDataKind(dataKind: Endpoint.DataKind): DeviceDriver.Output<T> {
            getBuilder().setDataKind(dataKind)
            return this
        }

        override fun setUserInteraction(interaction: Endpoint.Interaction): DeviceDriver.Output<T> {
            getBuilder().setUserInteraction(interaction)
            return this
        }

        override fun setUnits(units: Units): DeviceDriver.Output<T> {
            getBuilder().setUnits(units)
            return this
        }

        override fun set(update: T) {
            val runtime = getRuntime()
            context.run {
                runtime.set(update)
            }
        }

        fun onFinishBuilding(): Endpoint<T> {
            val endpoint = getBuilder().build()
            val runtime = when (endpoint.retention) {
                Endpoint.Retention.RETAINED -> StatefulRuntime(endpoint)
                Endpoint.Retention.NOT_RETAINED -> StatelessRuntime(endpoint)
            }
            log.v("Using ${runtime::class.simpleName} runtime")
            this.runtime = runtime
            builder = null
            return endpoint
        }

        fun onStartInteraction() {
            getRuntime().unlock()
        }

        private fun <D> Endpoint<D>.fireUpdate(update: D) {
            brokerQueue.execute {
                network.publish(context.id, this, update)
                        .waitForCompletion(1000)
            }
        }

        inner class StatelessRuntime<T>(private val endpoint: Endpoint<T>) : OutputRuntime<T> {
            override fun set(update: T) {
                endpoint.fireUpdate(update)
            }

            override fun unlock() {
                // do nothing
            }
        }

        inner class StatefulRuntime<T>(private val endpoint: Endpoint<T>) : OutputRuntime<T> {
            private var valueIsSet = false
            private var locked = true
            private var retainedValue: T? = null

            private fun retainValue(update: T) {
                valueIsSet = true
                retainedValue = update
            }

            private fun postingSameAsRetained(update: T) = valueIsSet && update == retainedValue

            override fun set(update: T) {
                if (locked) {
                    log.w("Update not fired - output is locked. Update was $update")
                } else if (!postingSameAsRetained(update)) {
                    retainValue(update)
                    endpoint.fireUpdate(update)
                }
            }

            override fun unlock() {
                if (locked) {
                    locked = false
                }
            }
        }
    }

    private interface OutputRuntime<T> {
        fun set(update: T)
        fun unlock()
    }

    inner class InputImpl<T>(
            private val context: DeviceThreadContext,
            private val id: Endpoint.Id,
            type: Endpoint.Type<T>,
            private val log: Log
    ) : DeviceDriver.Input<T> {

        private var builder: EndpointBuilder<T>? = EndpointBuilder(id, type, Endpoint.Direction.INPUT)
        private var observer: (T) -> Unit = {}
        private var endpoint: Endpoint<T>? = null

        private fun getBuilder(): EndpointBuilder<T> {
            return builder ?: throw IllegalStateException("Already finalized")
        }

        override fun observe(observer: (T) -> Unit) {
            this.observer = observer
        }

        fun onFinishBuilding(): Endpoint<T> {
            val endpoint = getBuilder().build()
            builder = null
            this.endpoint = endpoint
            return endpoint
        }

        fun onStartInteraction() {
            val endpoint = this.endpoint
            if (endpoint != null) {
                performSubscribe(endpoint)
            } else {
                throw IllegalStateException("Not yet finalized")
            }
        }

        private fun performSubscribe(endpoint: Endpoint<T>) {
            brokerQueue.execute {
                network.subscribe(context.id, endpoint) { _, _, data ->
                    context.run {
                        context.notifyInputGained(endpoint.id)
                        observer(data)
                    }
                }
                log.v("Subscribed to ${context.id}/${endpoint.id}")
            }
        }

        override fun setDataKind(dataKind: Endpoint.DataKind): DeviceDriver.Input<T> {
            getBuilder().setDataKind(dataKind)
            return this
        }

        override fun setUserInteraction(interaction: Endpoint.Interaction): DeviceDriver.Input<T> {
            getBuilder().setUserInteraction(interaction)
            return this
        }

        override fun setUnits(units: Units): DeviceDriver.Input<T> {
            getBuilder().setUnits(units)
            return this
        }

        override fun waitForDataBeforeOutput(): DeviceDriver.Input<T> {
            context.waitFotInput(id)
            return this
        }
    }
}

private class EndpointBuilder<T>(
        private val id: Endpoint.Id,
        private val type: Endpoint.Type<T>,
        private val direction: Endpoint.Direction
) {

    private var units: Units = Units.NO
    private var dataKind: Endpoint.DataKind = Endpoint.DataKind.CURRENT
    private var interaction: Endpoint.Interaction = Endpoint.Interaction.INVISIBLE

    fun setDataKind(dataKind: Endpoint.DataKind) {
        this.dataKind = dataKind
    }

    fun setUserInteraction(interaction: Endpoint.Interaction) {
        this.interaction = interaction
    }

    fun setUnits(units: Units) {
        this.units = units
    }

    fun build(): Endpoint<T> {
        val retention = if (direction == Endpoint.Direction.INPUT
                || dataKind == Endpoint.DataKind.EVENT) {
            Endpoint.Retention.NOT_RETAINED
        } else {
            type.accept(InferOutputRetentionVisitor())
        }

        return Endpoint(
                id,
                type,
                direction,
                retention,
                dataKind,
                interaction,
                units
        )
    }
}

private class InferOutputRetentionVisitor
    : Endpoint.Type.DefaultVisitor<Endpoint.Retention>(Endpoint.Retention.RETAINED) {

    override fun onAction(type: Endpoint.Type<Int>): Endpoint.Retention {
        return Endpoint.Retention.NOT_RETAINED
    }
}