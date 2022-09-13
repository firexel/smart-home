package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.threading.ThreadExecutor
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.WildcardBroker
import com.seraph.smarthome.util.ConsoleLog
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.regex.Pattern

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {

        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("WB")
            val config = readConfig(File("config.json"))
            val wbBroker = Brokers.unencrypted(
                config.wirenboard.address.toString(),
                config.wirenboard.name,
                log.copy("WBBroker"),
                config.wirenboard.credentials?.login,
                config.wirenboard.credentials?.passwd,
                randomizeName = true
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
            val drivers =
                DriversManager(network, Device.Id(config.smarthome.name), log = log.copy("Drivers"))
            val bridge = WirenboardBridge(
                wbWildcardBroker,
                drivers,
                log.copy("Bridge"),
                makeFilters(config)
            )
            runBlocking { bridge.serve(this) }
        }

        private fun makeFilters(config: Config): List<WirenboardBridge.DeviceInfoFilter> {
            return config.devices.map { (id, filter) ->
                RegexDeviceFilter(id, filter)
            }
        }

        private class RegexDeviceFilter(id: String, private val filter: DeviceFilter) :
            WirenboardBridge.DeviceInfoFilter {

            private val idPattern = Regex(id)

            override fun filter(info: WirenboardBridge.DeviceInfo): WirenboardBridge.DeviceInfo? {
                var newInfo = info
                if (info.wbId.matches(idPattern)) {
                    if (filter.exclude) {
                        return null
                    } else {
                        if (filter.rename != null) {
                            newInfo = newInfo.copy(wbId = filter.rename)
                        }
                        if (filter.endpoints.isNotEmpty()) {
                            filter.endpoints.forEach { (id, filter) ->
                                val endPattern = Regex(id)
                                newInfo =
                                    newInfo.copy(controls = newInfo.controls.mapNotNull { control ->
                                        if (control.id.matches(endPattern)) {
                                            if (filter.exclude) {
                                                null
                                            } else if (filter.rename != null) {
                                                control.rename(filter.rename)
                                                control
                                            } else {
                                                control
                                            }
                                        } else {
                                            control
                                        }
                                    })
                            }
                        }
                    }
                }
                return newInfo
            }
        }
    }
}
