package com.seraph.connector.configuration

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.NetworkMonitor
import script.definition.*
import java.time.LocalDateTime
import kotlin.reflect.KClass

class ConfigChecker(
    private val monitor: NetworkMonitor
) : EvalConfigInstaller.CheckingTreeBuilder {

    private var _results: MutableList<ExecutionNote> = mutableListOf()
    override val results: List<ExecutionNote>
        get() = _results

    private fun report(severity: ExecutionNote.Severity, message: String) {
        _results.add(ExecutionNote(severity, message))
    }

    override fun <T : Any> input(devId: String, endId: String, type: KClass<T>): Consumer<T> {
        checkEndpointMatching(devId, endId, type, Endpoint.Direction.INPUT)
        return mockConsumer()
    }

    override fun <T : Any> output(devId: String, endId: String, type: KClass<T>): Producer<T> {
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

    override fun <T : Any> constant(value: T): Producer<T> {
        return mockProducer()
    }

    override fun <T : Any> map(block: suspend MapContext.() -> T): Producer<T> {
        return mockProducer()
    }

    override fun <T : Any> Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit) {
        // do nothing
    }

    fun <T> mockConsumer(): Consumer<T> {
        return object : Consumer<T> {
            override var value: Producer<T>?
                get() = null
                set(value) {}
        }
    }

    fun <T> mockProducer(): Producer<T> {
        return object : Producer<T> {}
    }

    override fun timer(tickInterval: Long, stopAfter: Long): Timer {
        return object : Timer {
            override fun start() {}

            override fun stop() {}

            override val active: Producer<Boolean>
                get() = mockProducer()

            override val millisPassed: Producer<Long>
                get() = mockProducer()
        }
    }

    override fun clock(tickInterval: Clock.Interval): Clock {
        return object : Clock {
            override val time: Producer<LocalDateTime>
                get() = mockProducer()
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