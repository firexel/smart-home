package com.seraph.smarthome.logic

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.logic.devices.*
import com.seraph.smarthome.logic.scenes.FactorMapper
import com.seraph.smarthome.logic.scenes.RegionMapper
import com.seraph.smarthome.logic.scenes.Scene
import com.seraph.smarthome.logic.scenes.ScenesManager
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import java.io.File
import java.io.FileReader
import java.util.*
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
            val broker = Brokers.unencrypted(params.brokerAddress, "Logic Gates Service", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val manager = DriversManager(network, Device.Id("logic"))
            val configNode = readConfig(FileReader(params.configFile), ::driverSettings)
            val timer = Timer()


            val scenery = ScenesManager()
            scenery.registerScene("entrance", listOf(
                    Scene.Channel("light_03"),
                    Scene.Channel("light_45", FactorMapper(0.45f))
            ))
            scenery.registerScene("livingroom", listOf(
                    Scene.Channel("light_45", FactorMapper(0.55f)),
                    Scene.Channel("light_48", RegionMapper(0.2f)),
                    Scene.Channel("light_49"),
                    Scene.Channel("light_50", RegionMapper(0.2f)),
                    Scene.Channel("light_51")
            ))
            scenery.registerScene("alex_workplace", listOf(
                    Scene.Channel("light_46_47")
            ))
            scenery.registerScene("bedroom", listOf(
                    Scene.Channel("light_70_71")
            ))
            scenery.registerScene("bedroom_star", listOf(
                    Scene.Channel("light_68")
            ))
            scenery.registerScene("bed_alex", listOf(
                    Scene.Channel("light_73")
            ))
            scenery.registerScene("bed_ntsh", listOf(
                    Scene.Channel("light_75")
            ))
            scenery.bind("scenery", manager)

            configNode.devices.forEach {
                manager.addDriver(Device.Id(it.key), it.value.instantiateDevice(timer))
            }
        }
    }
}

private fun DeviceNode.instantiateDevice(timer: Timer): DeviceDriver {
    return when (Drivers.valueOf(driver)) {
        Drivers.SWITCH -> Switch()
        Drivers.BUTTON -> Button(timer)
        Drivers.DIMMER -> Dimmer()
        Drivers.SPLITTER -> Splitter()
        Drivers.THERMOSTAT -> Thermostat(settings as Thermostat.Settings)
    }
}

enum class Drivers(val settingsClass: KClass<*>?) {
    SWITCH(null),
    BUTTON(null),
    DIMMER(null),
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