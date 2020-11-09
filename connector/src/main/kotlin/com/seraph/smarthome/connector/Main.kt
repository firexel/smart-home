package com.seraph.smarthome.connector

import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

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
            val connections = readConfig(File("config.json")).mapToConnectionsList()
            val broker = Brokers.unencrypted(params.brokerAddress, "Connector", log.copy("Broker"))
            val network = MqttNetwork(LocalBroker(broker), log.copy("Network"))
            Connector(network, connections, log).serve()
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}
