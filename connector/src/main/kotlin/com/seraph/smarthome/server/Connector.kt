package com.seraph.smarthome.server

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.util.Log

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

    public fun serve() {
        network.subscribe { device: Device ->
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

    class MakeConnectionVisitor(
            private val network: Network,
            private val src: GlobalEndpointId,
            private val dst: GlobalEndpointId,
            private val dstEndpoint: Endpoint<*>
    ) : Endpoint.Visitor<Unit> {

        override fun onVoid(endpoint: Endpoint<Unit>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<Unit>, data)
            }
        }

        override fun onBoolean(endpoint: Endpoint<Boolean>) {
            network.subscribe(src.device, endpoint) { _, _, data ->
                network.publish(dst.device, dstEndpoint as Endpoint<Boolean>, data)
            }
        }
    }

    data class GlobalEndpointId(
            val device: Device.Id,
            private val endpoint: Endpoint.Id
    ) {
        override fun toString(): String = "$device::$endpoint"
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
    }
}