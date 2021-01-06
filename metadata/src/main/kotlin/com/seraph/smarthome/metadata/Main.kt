package com.seraph.smarthome.metadata

import com.seraph.smarthome.domain.Metainfo
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Metainfo").apply { i("Starting...") }
            val params = CommandLineParams(ArgParser(argv))
            val broker = Brokers.unencrypted(params.brokerAddress, "MetadataService", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val metainfo: Metainfo = Json.decodeFromString(FileReader(File("config.json")).readText())
            network.publish(metainfo)
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}