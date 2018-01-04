package com.seraph.smarthome.logic

import com.seraph.smarthome.logic.devices.VirtualOverrideSwitch
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Logic")
            val params = CommandLineParams(ArgParser(argv))
            val broker = StatefulMqttBroker(params.brokerAddress, "Logic Gates Service", log.copy("Broker"))
            val manager = DeviceManager(broker, log.copy("DeviceManager"))

            manager.addDevice(VirtualOverrideSwitch())
            manager.addDevice(VirtualOverrideSwitch())
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}