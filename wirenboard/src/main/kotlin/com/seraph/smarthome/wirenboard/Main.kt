package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.ThreadExecutor
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.concurrent.thread

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {

        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("WB")
            val config = readConfig(File("config.json"))
            val wbBroker = LocalBroker(
                Brokers.unencrypted(
                    config.wirenboard.address.toString(),
                    config.wirenboard.name,
                    log.copy("WBBroker"),
                    config.wirenboard.credentials?.login,
                    config.wirenboard.credentials?.passwd,
                    randomizeName = true
                )
            )
            val wbWildcardBroker = WildcardBroker(
                wbBroker, ThreadExecutor(), log.copy("Wildcard"),
                listOf(
                    WirenboardTopics.device(),
                    WirenboardTopics.control(),
                    WirenboardTopics.control() + "+",
                    (WirenboardTopics.control() + "+") + "+"
                )
            )
            val shBroker = Brokers.unencrypted(
                config.smarthome.address.toString(),
                config.smarthome.name,
                log.copy("SHBroker"),
                config.smarthome.credentials?.login,
                config.smarthome.credentials?.passwd,
            )
            val network = MqttNetwork(shBroker, log.copy("Network"))
            val drivers = DriversManager(network, Device.Id(config.smarthome.name), log = log.copy("Drivers"))
            val bridge = WirenboardBridge(
                wbWildcardBroker,
                drivers,
                log.copy("Bridge"),
                makeFilters(config)
            )
            runBlocking { bridge.serve(this) }
        }

        private fun makeFilters(config: Config): List<WirenboardBridge.DeviceInfoFilter> {
            val list = mutableListOf<WirenboardBridge.DeviceInfoFilter>()
            if (config.exclude.devices.isNotEmpty()) {
                list += WirenboardBridge.filterOutByDeviceId(config.exclude.devices)
            }
            if (config.exclude.endpoints.isNotEmpty()) {
                list += WirenboardBridge.filterOutEndpointsById(config.exclude.endpoints)
            }
            list += config.rename.map { WirenboardBridge.changeDeviceId(it.id, it.name) }
            return list
        }
    }
}
