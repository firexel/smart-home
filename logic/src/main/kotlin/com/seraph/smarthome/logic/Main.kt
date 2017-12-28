package com.seraph.smarthome.logic

import com.seraph.smarthome.logic.devices.VirtualSwitch
import com.seraph.smarthome.model.ConsoleLog
import com.seraph.smarthome.model.MqttBroker
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
            val broker = MqttBroker(params.brokerAddress, "Logic Gates Service", ConsoleLog())
            val manager = DeviceManager(broker)
            manager.addDevice(VirtualSwitch())
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}