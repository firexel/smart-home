package com.seraph.smarthome.stat

import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.EndpointSnapshot
import com.seraph.smarthome.util.NetworkEvent
import com.seraph.smarthome.util.NetworkMonitor
import java.io.File

/**
 * Created by aleksandr.naumov on 09.10.2020
 */

class Main {
    companion object {

        private lateinit var senders: List<StatSender>

        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Stat").apply { i("Starting...") }
            val config = readConfig(File("config.json"))
            log.i("Config is $config")

            val broker = Brokers.unencrypted(config.network.toString(), "Stat", log.copy("Broker"))
            val network = MqttNetwork(LocalBroker(broker), log.copy("Network"))

            senders = config.outputs.map {
                when (it) {
                    is GraphiteOutput -> GraphiteStatSender(
                            it.address.host, it.address.port, log.copy("GraphiteSender"))
                }
            }

            NetworkMonitor(network, log.copy("NetworkMonitor"), recordEvents = true) {
                val events = it.events
                events.forEach { event ->
                    when (event) {
                        is NetworkEvent.EndpointUpdated<*> -> sendMetrics(event.endpoint)
                        is NetworkEvent.EndpointAdded<*> -> sendMetrics(event.endpoint)
                        is NetworkEvent.EndpointRemoved<*> -> sendMetrics(event.endpoint)
                    }
                }
            }
        }

        private fun sendMetrics(endpoint: EndpointSnapshot<*>) {
            if (endpoint.isSet) {
                val valueAsDouble = convertToDouble(endpoint)
                val metricName = extractMetricName(endpoint)
                val timestamp = System.currentTimeMillis() / 1000
                senders.forEach {
                    it.enqueue(metricName, valueAsDouble, timestamp)
                }
            }
        }

        private fun extractMetricName(endpoint: EndpointSnapshot<*>): StatSender.MetricName {
            val direction = when (endpoint.endpoint.direction) {
                Endpoint.Direction.INPUT -> "inputs"
                Endpoint.Direction.OUTPUT -> "outputs"
            }
            return StatSender.MetricName(listOf(
                    "home",
                    endpoint.device.id.toString().replace(":", "_"),
                    direction,
                    endpoint.endpoint.id.toString()
            ))
        }

        private fun convertToDouble(endpoint: EndpointSnapshot<*>): Double {
            val value = endpoint.value
            return if (value == null) {
                Double.NaN
            } else {
                endpoint.endpoint.accept(MetricCastVisitor(value))
            }
        }
    }
}

class MetricCastVisitor(private val value: Any) : Endpoint.Visitor<Double> {
    override fun onInt(endpoint: Endpoint<Int>): Double {
        return (value as Int).toDouble()
    }

    override fun onAction(endpoint: Endpoint<Int>): Double {
        return Double.NaN
    }

    override fun onBoolean(endpoint: Endpoint<Boolean>): Double {
        return when (value as Boolean) {
            true -> 1.0
            false -> 0.0
        }
    }

    override fun onFloat(endpoint: Endpoint<Float>): Double {
        return (value as Float).toDouble()
    }

    override fun onDeviceState(endpoint: Endpoint<DeviceState>): Double {
        return (value as DeviceState).ordinal.toDouble()
    }
}
