package com.seraph.smarthome.connector

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.util.Log
import java.util.*

/**
 * Created by aleksandr.naumov on 02.12.2017.
 */

class Connector(
        private val network: Network,
        connections: List<Connection>,
        private val log: Log
) {

    private val pendingConnections = mutableListOf<PendingConnection>().apply {
        addAll(connections.map {
            PendingConnection(
                    GlobalEndpointId(it.srcDevice, it.srcEndpoint),
                    GlobalEndpointId(it.dstDevice, it.dstEndpoint)
            )
        })
    }

    private val timer = Timer("Connection watchdog")

    public fun serve() {
        val reportTask = object : TimerTask() {
            override fun run() {
                synchronized(pendingConnections) {
                    if (pendingConnections.isNotEmpty()) {
                        val unresolvedSet = pendingConnections
                                .map { it.unresolvedGlobalEndpoints }
                                .toSet()

                        log.w("Still waiting for $unresolvedSet")
                    } else {
                        log.i("All connections are successfully resolved")
                    }
                }
            }
        }
        timer.schedule(reportTask, 5000)
        network.subscribe(null) { device: Device ->
            device.endpoints.forEach { endpoint ->
                val gid = GlobalEndpointId(device.id, endpoint.id)
                synchronized(pendingConnections) {
                    pendingConnections.forEach {
                        if (it.src == gid) {
                            it.srcEndpoint = endpoint
                        }
                        if (it.dst == gid) {
                            it.dstEndpoint = endpoint
                        }
                    }
                }
            }
            synchronized(pendingConnections) {
                val readyToMakeConnection = pendingConnections.filter {
                    it.srcEndpoint != null && it.dstEndpoint != null
                }
                readyToMakeConnection.forEach { it.makeConnection(network, log) }
                pendingConnections.removeAll(readyToMakeConnection)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class MakeConnectionVisitor(
            private val network: Network,
            private val src: GlobalEndpointId,
            private val dst: GlobalEndpointId,
            private val dstEndpoint: Endpoint<*>
    ) : Endpoint.Visitor<Unit> {
        override fun onInt(endpoint: Endpoint<Int>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<Int>, data)
            }
        }

        override fun onAction(endpoint: Endpoint<Int>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<Int>, data)
            }
        }

        override fun onBoolean(endpoint: Endpoint<Boolean>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<Boolean>, data)
            }
        }

        override fun onFloat(endpoint: Endpoint<Float>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<Float>, data)
            }
        }

        override fun onDeviceState(endpoint: Endpoint<DeviceState>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<DeviceState>, data)
            }
        }
    }

    data class PendingConnection(
            val src: GlobalEndpointId,
            val dst: GlobalEndpointId,
            var srcEndpoint: Endpoint<*>? = null,
            var dstEndpoint: Endpoint<*>? = null
    ) {
        fun makeConnection(network: Network, log: Log) {
            val srcEndpoint = srcEndpoint
            val dstEndpoint = dstEndpoint

            if (srcEndpoint == null || dstEndpoint == null) {
                throw IllegalStateException()
            } else if (srcEndpoint.type != dstEndpoint.type) {
                log.w("$src and $dst has incompatible types:\n")
            } else {
                srcEndpoint.accept(MakeConnectionVisitor(network, src, dst, dstEndpoint))
            }
        }

        val unresolvedGlobalEndpoints: List<GlobalEndpointId>
            get() {
                return listOf<GlobalEndpointId>() +
                        if (srcEndpoint == null) listOf(src) else emptyList<GlobalEndpointId>() +
                                if (dstEndpoint == null) listOf(dst) else emptyList()
            }
    }
}