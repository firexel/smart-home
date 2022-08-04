package com.seraph.smarthome.bridge

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.*
import java.io.File
import java.util.*

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {

        private lateinit var networksMap: Map<String, List<NetworkBundle>>

        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Bridge")
            val config = readConfig(File("config.json"))
            val bundles = constructNetworkBundles(config, log)
            networksMap = mapNetworksToBundles(config, bundles)
            startMonitoring(bundles, log)
        }

        private fun startMonitoring(bundles: Map<String, NetworkBundle>, log: ConsoleLog) {
            bundles.forEach { entry ->
                if (networksMap.containsKey(entry.key)) {
                    entry.value.monitor.subscribe { monitor ->
                        handleNetworkUpdate(entry.key, monitor)
                    }
                } else {
                    log.w("Network ${entry.key} not connected to any other network and will not be monitored")
                }
            }
        }

        private fun mapNetworksToBundles(config: Config, bundles: Map<String, NetworkBundle>): Map<String, List<NetworkBundle>> {
            val allNetworks = config.routes.flatMap { listOf(it.from, it.to) }.toSet()
            return allNetworks.associateWith { name ->
                config.routes
                        .filter { it.from == name || it.to == name }
                        .map {
                            if (it.from == name) {
                                it.to
                            } else {
                                it.from
                            }
                        }
                        .distinct()
                        .map {
                            bundles[it]
                                    ?: throw IllegalArgumentException("Network $it is not described")
                        }
            }
        }

        private fun constructNetworkBundles(config: Config, log: ConsoleLog): Map<String, NetworkBundle> {
            return config.networks.mapValues {
                val creds = it.value.credentials
                val name = it.key.capitalize(Locale.ENGLISH)
                val broker = createBroker(creds, log.copy("${name}Broker"), it.value.address)
                val network = MqttNetwork(LocalBroker(broker), log.copy("${name}Network"))
                val monitor = NetworkMonitor(
                        network, log.copy("${name}Monitor"),
                        recordEvents = true
                )
                NetworkBundle(it.key, network, monitor)
            }
        }

        private fun createBroker(creds: Credentials?, log: ConsoleLog, address: Address): Broker {
            return if (creds != null) {
                Brokers.unencrypted(
                        address.toString(),
                        "Bridge",
                        log,
                        userName = creds.login,
                        userPswd = creds.passwd
                )
            } else {
                Brokers.unencrypted(address.toString(), "Bridge", log)
            }
        }

        private fun handleNetworkUpdate(name: String, monitor: NetworkMonitor) {
            monitor.events.forEach {
                when (it) {
                    is NetworkEvent.EndpointUpdated<*> -> if (it.endpoint.isSet) {
                        postEndpoint(name, it.endpoint)
                    }
                    is NetworkEvent.EndpointAdded<*> -> if (it.endpoint.isSet) {
                        postEndpoint(name, it.endpoint)
                    }
                    is NetworkEvent.DeviceAdded -> postDevice(name, it.device)
                    is NetworkEvent.DeviceUpdated -> postDevice(name, it.device)
                    is NetworkEvent.MetainfoUpdated -> postMetainfo(name, it.metainfo)
                }
            }
        }

        private fun postEndpoint(name: String, snapshot: EndpointSnapshot<*>) {
            fun <T> Endpoint<T>.publish(network: MqttNetwork, device: Device.Id, obj: Any) {
                network.set(device, this, this.cast(obj))
            }
            eachConnectedNetwork(name) { network, monitor ->
                val foreign = monitor.snapshot(snapshot.device.id, snapshot.endpoint.id)
                if (foreign?.value != snapshot.value) {
                    snapshot.endpoint.publish(network, snapshot.device.id, snapshot.value!!)
                }
            }
        }

        private fun postDevice(name: String, snapshot: DeviceSnapshot) {
            eachConnectedNetwork(name) { network, monitor ->
                val foreign = monitor.snapshot(snapshot.device.id)
                if (foreign?.device != snapshot.device) {
                    network.publish(snapshot.device)
                }
            }
        }

        private fun postMetainfo(name: String, metainfo: Metainfo) {
            eachConnectedNetwork(name) { network, monitor ->
                val foreign = monitor.snapshot()
                if (foreign.metainfo != metainfo) {
                    network.publish(metainfo)
                }
            }
        }

        private fun eachConnectedNetwork(name: String, actor: (MqttNetwork, NetworkMonitor) -> Unit) {
            (networksMap[name] ?: error("Unknown network $name")).forEach { bundle ->
                actor(bundle.network, bundle.monitor)
            }
        }
    }

    data class NetworkBundle(
            val name: String,
            val network: MqttNetwork,
            val monitor: NetworkMonitor
    )
}
