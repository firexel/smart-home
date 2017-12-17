package com.seraph.smarthome.metadata

import com.google.gson.Gson
import com.seraph.smarthome.model.*
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val params = CommandLineParams(ArgParser(argv))
            val broker = MqttBroker(params.brokerAddress, "MetadataService", ConsoleLog())
            putMetadata(broker, params.brokerName)
        }

        private fun putMetadata(broker: MqttBroker, brokerName: String) {
            broker.publish(
                    Topics.metadata(),
                    Gson().toJson(Metadata(brokerName))
            )
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")

    val brokerName by parser.storing("-n", "--name", help = "broker name to be stored in metadata")
            .default("Debug broker")
}