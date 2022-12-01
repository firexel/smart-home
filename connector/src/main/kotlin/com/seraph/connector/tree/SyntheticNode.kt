package com.seraph.connector.tree

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Serializer
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import script.definition.Synthetic
import script.definition.Units
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class SyntheticNode<T : Any>(
    private val id: String,
    type: KClass<T>,
    private val persistence: Synthetic.Persistence<T>,
    private val access: Synthetic.ExternalAccess,
    private val units: Units,
    private val storage: Storage,
    private val driverManager: DriversManager,
    private val log: Log
) : Synthetic<T>, Node {

    val type: Endpoint.Type<T> =
        SyntheticDeviceDriver.inferType(type)
            ?: throw Node.NodeInitException("Unsupported type $type")

    override val output = StateFlowProducerNode<T>(this, null)
    override val input = StateFlowConsumerNode<T>(this, null)

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            persistence.load()?.also { input.stateFlow.value = it }
            val driver = registerDevice()
            try {
                input.stateFlow.filterNotNull().collect {
                    withContext(NonCancellable) {
                        driver.transmit(it)
                        if (persistence !is Synthetic.Persistence.None) {
                            storage.put("value", type.serializer.toBytes(it))
                        }
                    }
                }
            } finally {
                withContext(NonCancellable) { unregisterDevice() }
            }
        }
    }

    private suspend fun Synthetic.Persistence<T>.load(): T? {
        return when (this) {
            is Synthetic.Persistence.Stored<T> -> {
                try {
                    storage.get("value")?.let { type.serializer.fromBytes(it) }
                } catch (ex: Serializer.TypeMismatchException) {
                    log.w("Value stored in storage cannot be cast to a $type")
                    storage.put("value", default?.let { type.serializer.toBytes(it) })
                    default
                }
            }
            is Synthetic.Persistence.Runtime<T> -> default
            else -> null
        }
    }

    private suspend fun registerDevice(): SyntheticDeviceDriver<T> {
        val driver = SyntheticDeviceDriver(
            access,
            output.stateFlow,
            type,
            log,
            units
        )
        driverManager.addDriver(Device.Id(id), driver, SyntheticDeviceDriver.executor)
        return driver
    }

    private suspend fun unregisterDevice() {
        // unsupported
    }

    private class SyntheticDeviceDriver<T : Any>(
        private val access: Synthetic.ExternalAccess,
        private val feedback: MutableStateFlow<T?>,
        private val type: Endpoint.Type<T>,
        private val log: Log,
        units: Units
    ) : DeviceDriver {

        val units = try {
            com.seraph.smarthome.domain.Units.valueOf(units.name)
        } catch (ex: IllegalArgumentException) {
            log.w("Unknown units $units")
            com.seraph.smarthome.domain.Units.NO
        }

        var output: DeviceDriver.Output<T>? = null

        fun transmit(data: T) = executor.execute {
            output?.set(data)
        }

        override fun bind(visitor: DeviceDriver.Visitor) {
            if (access == Synthetic.ExternalAccess.READ || access == Synthetic.ExternalAccess.READ_WRITE) {
                output = visitor.declareOutput("out", type)
                    .setUnits(units)
            }
            if (access == Synthetic.ExternalAccess.WRITE || access == Synthetic.ExternalAccess.READ_WRITE) {
                visitor.declareInput("in", type)
                    .setUnits(units)
                    .observe { feedback.value = it }
            }
        }

        @Suppress("UNCHECKED_CAST")
        companion object {

            val executor: Executor = Executors.newFixedThreadPool(1)

            fun <T : Any> inferType(klass: KClass<T>): Endpoint.Type<T>? {
                return when (klass) {
                    Float::class -> Types.FLOAT as Endpoint.Type<T>?
                    Int::class -> Types.INTEGER as Endpoint.Type<T>?
                    Boolean::class -> Types.BOOLEAN as Endpoint.Type<T>?
                    String::class -> Types.STRING as Endpoint.Type<T>?
                    Unit::class -> Types.ACTION as Endpoint.Type<T>?
                    else -> null
                }
            }
        }
    }
}