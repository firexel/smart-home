package com.seraph.connector.configuration

import com.seraph.connector.tree.Node
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import script.definition.*
import java.time.LocalDateTime
import kotlin.reflect.KClass

class ConfigChecker(
    private val monitor: NetworkMonitor
) : TreeBuilder {

    private var _results: MutableList<ExecutionNote> = mutableListOf()
    val results: List<ExecutionNote>
        get() = _results

    private fun report(severity: ExecutionNote.Severity, message: String) {
        _results.add(ExecutionNote(severity, message))
    }

    override fun <T : Any> Node.Producer<T>.transmitTo(consumer: Node.Consumer<T>) {
        // do nothing
    }

    override fun <T : Any> Node.Consumer<T>.receiveFrom(producer: Node.Producer<T>) {
        // do nothing
    }

    override fun <T : Any> Node.Consumer<T>.disconnect() {
        // do nothing
    }

    override fun <T : Any> input(devId: String, endId: String, type: KClass<T>): Node.Consumer<T> {
        checkEndpointMatching(devId, endId, type, Endpoint.Direction.INPUT)
        return mockConsumer()
    }

    override fun <T : Any> output(devId: String, endId: String, type: KClass<T>): Node.Producer<T> {
        checkEndpointMatching(devId, endId, type, Endpoint.Direction.OUTPUT)
        return mockProducer()
    }

    private fun <T : Any> checkEndpointMatching(
        devId: String,
        endId: String,
        type: KClass<T>,
        direction: Endpoint.Direction
    ) {
        val snapshot = monitor.snapshot()
        val dev = snapshot.devices[Device.Id(devId)]
        if (dev == null) {
            report(ExecutionNote.Severity.WARNING, "Device $devId not found in network")
            return
        }
        val endpoint = dev.endpoints[Endpoint.Id(endId)]
        if (endpoint == null) {
            report(ExecutionNote.Severity.WARNING, "Endpoint $devId/$endId not found")
            return
        }
        if (!endpoint.isSet) {
            report(ExecutionNote.Severity.WARNING, "Endpoint $devId/$endId exists but not set")
        }
        val matchingType = matchingType(type)
        if (matchingType == null) {
            report(ExecutionNote.Severity.ERROR, "Using unsupported type $type")
        } else if (endpoint.endpoint.type != matchingType) {
            report(
                ExecutionNote.Severity.ERROR,
                "Endpoint type mismatch. $devId/$endId has type ${endpoint.endpoint.type} " +
                        ", but requested type is $matchingType"
            )
        }
        if (endpoint.endpoint.direction != direction) {
            report(
                ExecutionNote.Severity.ERROR,
                "Endpoint direction mismatch. $devId/$endId is a ${endpoint.endpoint.direction} " +
                        ", but requested direction is a $direction"
            )
        }
    }

    override fun <T : Any> constant(value: T): Node.Producer<T> {
        return mockProducer()
    }

    override fun <T : Any> map(block: suspend RuntimeReadContext.() -> T): Node.Producer<T> {
        return mockProducer()
    }

    override fun <T : Any> Node.Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit) {
        // do nothing
    }

    override fun <T : Any> synthetic(
        devId: String,
        type: KClass<T>,
        access: Synthetic.ExternalAccess,
        units: Units,
        persistence: Synthetic.Persistence<T>
    ): Synthetic<T> = object : Synthetic<T> {
        override val output: Node.Producer<T>
            get() = mockProducer()
        override val input: Node.Consumer<T>
            get() = mockConsumer()

        override suspend fun run(scope: CoroutineScope) {
            // do nothing
        }
    }

    override fun <R, T> monitor(windowWidthMs: Long, aggregator: (List<R>) -> T?): Monitor<R, T> =
        object : Monitor<R, T> {
            override val output: Node.Producer<T>
                get() = mockProducer()
            override val input: Node.Consumer<R>
                get() = mockConsumer()

            override suspend fun run(scope: CoroutineScope) {
                // do nothing
            }
        }

    fun <T> mockConsumer(): Node.Consumer<T> {
        return object : Node.Consumer<T> {
            override val parent: Node
                get() = throw RuntimeException("Should not being called")

            override suspend fun consume(flow: StateFlow<T?>) {
                throw RuntimeException("Should not being called")
            }
        }
    }

    fun <T> mockProducer(): Node.Producer<T> {
        return object : Node.Producer<T> {
            override val parent: Node
                get() = throw RuntimeException("Should not being called")
            override val flow: Flow<T?>
                get() = throw RuntimeException("Should not being called")
        }
    }

    override fun timer(tickInterval: Long, stopAfter: Long): Timer {
        return object : Timer {
            override fun start() {}

            override fun stop() {}

            override val active: Node.Producer<Boolean>
                get() = mockProducer()

            override val millisPassed: Node.Producer<Long>
                get() = mockProducer()

            override suspend fun run(scope: CoroutineScope) {
                // nothing
            }
        }
    }

    override fun clock(tickInterval: Clock.Interval): Clock {
        return object : Clock {
            override val time: Node.Producer<LocalDateTime>
                get() = mockProducer()

            override suspend fun run(scope: CoroutineScope) {
                // nothing
            }
        }
    }
}

fun <T : Any> matchingType(type: KClass<T>): Endpoint.Type<*>? {
    return when (type) {
        Int::class -> Types.INTEGER
        String::class -> Types.STRING
        Unit::class -> Types.ACTION
        Float::class -> Types.FLOAT
        Boolean::class -> Types.BOOLEAN
        DeviceState::class -> Types.DEVICE_STATE
        else -> null
    }
}