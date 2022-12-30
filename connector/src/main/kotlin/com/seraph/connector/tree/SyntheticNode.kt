package com.seraph.connector.tree

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Serializer
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import script.definition.Synthetic
import script.definition.Units
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class SyntheticNode<T : Any>(
    private val id: String,
    type: KClass<T>,
    persistence: Synthetic.Persistence<T>,
    private val access: Synthetic.ExternalAccess,
    private val units: Units,
    storage: Storage,
    private val driverManager: DriversManager,
    private val log: Log
) : Synthetic<T>, Node {

    private val type: Endpoint.Type<T> = inferType(type)
        ?: throw Node.NodeInitException("Unsupported type $type")

    private val store: Store<T> = when (persistence) {
        is Synthetic.Persistence.None -> NoneStore()
        is Synthetic.Persistence.Runtime -> RuntimeStore(persistence.default)
        is Synthetic.Persistence.Stored -> PersistedStore(persistence.default, this.type, storage)
    }

    override val output = StateFlowProducerNode<T>(this, null)
    override val input = StateFlowConsumerNode<T>(this, null)

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            store.load()?.also { input.stateFlow.value = it }
            val driver = registerDevice()
            val jobs = listOf(
                scope.launch {
                    startReceivingDataFromTheGraph(driver)
                },
                scope.launch {
                    startReceivingDataFromTheNetwork(driver)
                }
            )
            try {
                jobs.forEach { it.join() }
            } finally {
                withContext(NonCancellable) { unregisterDevice() }
            }
        }
    }

    private suspend fun startReceivingDataFromTheNetwork(driver: SyntheticDeviceDriver<T>) {
        driver.feedback.filterNotNull().collect {
            withContext(NonCancellable) {
                log.v("Network --[$it]-> Graph")
                putReceivedValue(it, driver)
            }
        }
    }

    private suspend fun startReceivingDataFromTheGraph(driver: SyntheticDeviceDriver<T>) {
        input.stateFlow.filterNotNull().collect {
            withContext(NonCancellable) {
                log.v("Graph --[$it]-> Network")
                putReceivedValue(it, driver)
            }
        }
    }

    private suspend fun putReceivedValue(data: T, driver: SyntheticDeviceDriver<T>) {
        store.put(data)
        output.value = data
        driver.transmit(data)
    }

    private fun registerDevice(): SyntheticDeviceDriver<T> {
        val driver = SyntheticDeviceDriver(MutableStateFlow(output.stateFlow.value), type)
        driverManager.addDriver(Device.Id(id), driver, executor)
        return driver
    }

    private fun unregisterDevice() {
        // unsupported
    }

    private inner class SyntheticDeviceDriver<T : Any>(
        val feedback: MutableStateFlow<T?>,
        private val type: Endpoint.Type<T>
    ) : DeviceDriver {

        val units = try {
            com.seraph.smarthome.domain.Units.valueOf(this@SyntheticNode.units.name)
        } catch (ex: IllegalArgumentException) {
            log.w("Unknown units ${this@SyntheticNode.units}")
            com.seraph.smarthome.domain.Units.NO
        }

        var output: DeviceDriver.Output<T>? = null

        fun transmit(data: T) = executor.execute {
            output?.set(data)
        }

        override fun bind(visitor: DeviceDriver.Visitor) = when (access) {
            Synthetic.ExternalAccess.READ ->
                registerOutput(visitor)
            Synthetic.ExternalAccess.WRITE ->
                registerInput(visitor)
            Synthetic.ExternalAccess.READ_WRITE -> {
                registerOutput(visitor)
                registerInput(visitor)
            }
        }

        private fun registerInput(visitor: DeviceDriver.Visitor) {
            visitor.declareInput("in", type)
                .setUnits(units)
                .observe { data ->
                    feedback.value = data
                }
        }

        private fun registerOutput(visitor: DeviceDriver.Visitor) {
            output = visitor.declareOutput("out", type)
                .setUnits(units)
        }
    }

    @Suppress("UNCHECKED_CAST")
    companion object {

        val executor: Executor = Executors.newFixedThreadPool(1)

        fun <T : Any> inferType(klass: KClass<T>): Endpoint.Type<T>? = when (klass) {
            Float::class -> Types.FLOAT as Endpoint.Type<T>?
            Int::class -> Types.INTEGER as Endpoint.Type<T>?
            Boolean::class -> Types.BOOLEAN as Endpoint.Type<T>?
            String::class -> Types.STRING as Endpoint.Type<T>?
            Unit::class -> Types.ACTION as Endpoint.Type<T>?
            else -> null
        }
    }

    interface Store<T> {
        suspend fun put(value: T?)
        suspend fun load(): T?
    }

    class NoneStore<T> : Store<T> {
        override suspend fun put(value: T?) {
            // do nothing
        }

        override suspend fun load(): T? {
            return null
        }
    }

    class RuntimeStore<T>(private val defaultVal: T?) : Store<T> {
        override suspend fun put(value: T?) {
            // do nothing
        }

        override suspend fun load(): T? {
            return defaultVal
        }
    }

    inner class PersistedStore<T>(
        private val defaultVal: T?,
        private val type: Endpoint.Type<T>,
        private val storage: Storage
    ) : Store<T> {

        override suspend fun put(value: T?) {
            storage.put(STORAGE_KEY, value?.let(type.serializer::toBytes))
        }

        override suspend fun load(): T? {
            return try {
                storage.get(STORAGE_KEY)?.let { type.serializer.fromBytes(it) }
            } catch (ex: Serializer.TypeMismatchException) {
                log.w("Value stored in storage cannot be cast to a $type")
                storage.put(STORAGE_KEY, defaultVal?.let(type.serializer::toBytes))
                defaultVal
            }
        }
    }
}

private const val STORAGE_KEY = "value"
