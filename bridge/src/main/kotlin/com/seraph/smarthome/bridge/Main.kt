package com.seraph.smarthome.bridge

import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Bridge")
            val params = CommandLineParams(ArgParser(argv))
            val externalBroker = Brokers.encrypted(
                    addr = "ssl://mqtt.gbridge.io:8883",
                    name = "Bridge",
                    userName = "gbridge-u1876",
                    userPswd = "7RKB7WrFwp22SJ",
                    caFile = File("/etc/letsencrypt.jks"),
                    caPswd = "letsencrypt",
                    log = log
            )
            val internalBroker = Brokers.unencrypted(params.brokerAddress, "Bridge", log.copy("Broker"))
            val network = MqttNetwork(LocalBroker(internalBroker), log.copy("Network"))
            val deviceMngr = DriversManager(network, Device.Id("bridge"), log = log)

            val bridges = Bridges(deviceMngr, externalBroker, Topic("gBridge", "u1876"), log)
            bridges.addLightBridge(
                    Topic("living_room", "alex_workplace"),
                    Device.Id("living_room", "alex_workplace")
            )
            bridges.addLightBridge(
                    Topic("bedroom", "ambient"),
                    Device.Id("bedroom", "ambient")
            )
            bridges.addLightBridge(
                    Topic("living_room", "entrance"),
                    Device.Id("living_room", "entrance")
            )
            bridges.addLightBridge(
                    Topic("living_room", "ambient"),
                    Device.Id("living_room", "ambient")
            )
            bridges.addLightBridge(
                    Topic("bedroom", "bed_alex"),
                    Device.Id("bedroom", "bed_alex")
            )
            bridges.addLightBridge(
                    Topic("bedroom", "bed_ntsh"),
                    Device.Id("bedroom", "bed_ntsh")
            )
            bridges.addLightBridge(
                    Topic("bedroom", "star"),
                    Device.Id("bedroom", "star")
            )
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}
