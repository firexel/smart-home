package com.seraph.smarthome.client.repositories

import android.os.Build
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.WildcardBroker
import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NetworkMonitor

class MqttNetworkRepository(options: ConnectionOptions, log: Log) : NetworkRepository {

    override lateinit var network: Network
    override lateinit var monitor: NetworkMonitor

    init {
        val name = "Client ${Build.MANUFACTURER} ${Build.MODEL}"
        val addr = "tcp://${options.address}:${options.port}"
        val broker = if (options.credentials != null) {
            Brokers.unencrypted(
                addr, name,
                log.copy("Broker"),
                options.credentials.login,
                options.credentials.password,
            )
        } else {
            Brokers.unencrypted(addr, name, log.copy("Broker"))
        }
        network = MqttNetwork(WildcardBroker(broker), log.copy("Network"))
        monitor = NetworkMonitor(network, log.copy("Monitor"), recordEvents = false)
    }

    data class ConnectionOptions(
        val address: String,
        val port: Int,
        val credentials: Credentials?,
    ) {
        data class Credentials(
            val login: String,
            val password: String,
        )
    }
}