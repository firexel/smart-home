package com.seraph.smarthome.logic

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.logic.devices.Splitter
import com.seraph.smarthome.logic.devices.Switch
import com.seraph.smarthome.logic.devices.Thermostat
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import java.io.File
import java.io.FileReader
import kotlin.reflect.KClass

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
            val manager = DriversManager(network, Device.Id("logic"))
            val configNode = readConfig(FileReader(params.configFile), ::driverSettings)
            configNode.devices.forEach {
                manager.addDriver(Device.Id(it.key), it.value.instantiateDevice())
            }
        }
    }
}

private fun DeviceNode.instantiateDevice(): DeviceDriver {
    return when (Drivers.valueOf(driver)) {
        Drivers.SWITCH -> Switch()
        Drivers.SPLITTER -> Splitter()
        Drivers.THERMOSTAT -> Thermostat(settings as Thermostat.Settings)
    }
}

enum class Drivers(val settingsClass: KClass<*>?) {
    SWITCH(null),
    SPLITTER(null),
    THERMOSTAT(Thermostat.Settings::class)
}

fun driverSettings(name: String): DriverInfo {
    return DriverInfo(Drivers.valueOf(name).settingsClass)
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")

    val configFile by parser.storing("-c", "--config", help = "path to config") {
        File(this)
    }.default(File("/etc/logic.json")).addValidator {
        if (!value.exists()) {
            throw SystemExitException("Config not found at ${value.absoluteFile}", -1)
        }
    }
}