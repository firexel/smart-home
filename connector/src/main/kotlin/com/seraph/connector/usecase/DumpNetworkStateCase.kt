package com.seraph.connector.usecase

import com.seraph.smarthome.util.NetworkMonitor
import java.util.*

class DumpNetworkStateCase(
    private val monitor: NetworkMonitor
) {
    suspend fun run(): NetworkDump {
        val devices = monitor.snapshot().devices
            .mapKeys { it.key.toString() }
            .mapValues {
                val endpoints = it.value.endpoints
                    .mapKeys { it.key.toString() }
                    .mapValues {
                        EndpointDump(
                            it.value.endpoint.type.toString(),
                            it.value.endpoint.direction.name.lowercase(Locale.US),
                            it.value.value
                        )
                    }
                DeviceDump(endpoints)
            }
        return NetworkDump(devices)
    }

    data class NetworkDump(
        val devices: Map<String, DeviceDump>
    )

    data class DeviceDump(
        val endpoints: Map<String, EndpointDump>
    )

    data class EndpointDump(
        val type: String,
        val direction: String,
        val value: Any?
    )
}