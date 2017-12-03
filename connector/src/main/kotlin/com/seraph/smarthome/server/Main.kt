package com.seraph.smarthome.server

import com.google.gson.Gson
import com.seraph.smarthome.model.ConnectionsList
import com.seraph.smarthome.model.ConsoleLog
import com.seraph.smarthome.model.MqttBroker
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import java.io.File
import java.io.FileReader

/**
 * Created by aleksandr.naumov on 01.12.2017.
 */
class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val params = CommandLineParams(ArgParser(argv))
            val connections = Gson().fromJson(FileReader(params.configPath), ConnectionsList::class.java)
            Connector(MqttBroker(params.brokerAddress, "SHCS", ConsoleLog()), connections).serve()
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val configPath by parser.storing("-l", "--list", help = "path to list list") {
        File(this)
    }.addValidator {
        if (!value.exists()) {
            throw SystemExitException("Connections list not found at ${value.absoluteFile}", -1)
        }
    }

    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
    // at iot.eclipse.org, port 1883
}
