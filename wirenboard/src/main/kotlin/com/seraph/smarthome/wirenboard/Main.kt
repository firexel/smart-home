package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {

        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("WB")
            val config = readConfig(File("config.json"))
            val wbBroker = LocalBroker(Brokers.unencrypted(
                    config.wirenboard.address.toString(),
                    config.name,
                    log.copy("WBBroker"),
                    config.wirenboard.credentials?.login,
                    config.wirenboard.credentials?.passwd,
            ))
            val shBroker = Brokers.unencrypted(
                    config.smarthome.address.toString(),
                    config.name,
                    log.copy("SHBroker"),
                    config.smarthome.credentials?.login,
                    config.smarthome.credentials?.passwd,
            )
            val network = MqttNetwork(shBroker, log.copy("Network"))
            val drivers = DriversManager(network, Device.Id(config.name), log = log.copy("Drivers"))
            val bridge = WirenboardBridge(wbBroker, drivers, log.copy("Bridge"))
            runBlocking { bridge.serve(this) }
        }
    }
}
