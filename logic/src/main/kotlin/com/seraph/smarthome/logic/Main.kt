package com.seraph.smarthome.logic

import com.seraph.smarthome.device.DeviceManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.logic.devices.OverrideSwitchDriver
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Logic").apply { i("Starting...") }
            val params = CommandLineParams(ArgParser(argv))
            val broker = StatefulMqttBroker(params.brokerAddress, "Logic Gates Service", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val manager = DeviceManager(network, Device.Id("logic"))

            manager.addDriver(Device.Id("switch"), OverrideSwitchDriver())
            manager.addDriver(Device.Id("switch"), OverrideSwitchDriver())
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}