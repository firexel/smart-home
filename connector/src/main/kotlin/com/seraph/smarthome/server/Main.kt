package com.seraph.smarthome.server

import com.google.gson.GsonBuilder
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.domain.impl.installModelAdapters
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import java.io.File
import java.io.FileReader

/**
 * Created by aleksandr.naumov on 01.12.2017.
 */
class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Connector").apply { i("Starting...") }
            log.i("Started with following params: ${argv.asList()}")
            val params = CommandLineParams(ArgParser(argv))
            val gsonBuilder = GsonBuilder()
            installModelAdapters(gsonBuilder)
            val gson = gsonBuilder.create()
            val connections = gson.fromJson(FileReader(params.configPath), ConnectionsList::class.java)
            val broker = StatefulMqttBroker(params.brokerAddress, "SHCS", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            Connector(network, connections.connections, log).serve()
        }
    }
}

data class ConnectionsList(val connections: List<Connection>)
data class Connection(
        val srcDevice: Device.Id,
        val srcEndpoint: Endpoint.Id,
        val dstDevice: Device.Id,
        val dstEndpoint: Endpoint.Id
)

class CommandLineParams(parser: ArgParser) {
    val configPath by parser.storing("-l", "--list", help = "path to list list") {
        File(this)
    }.default(File("../testdata/connections.json")).addValidator {
        if (!value.exists()) {
            throw SystemExitException("Connections list not found at ${value.absoluteFile}", -1)
        }
    }

    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}
