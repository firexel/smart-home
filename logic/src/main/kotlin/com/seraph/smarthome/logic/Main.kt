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
import com.seraph.smarthome.util.Scheduler
import com.seraph.smarthome.util.ThreadScheduler
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
            val broker = Brokers.unencrypted(params.brokerAddress, "Logic Gates Service", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val manager = DriversManager(network, Device.Id("logic"))
            val configNode = readConfig(FileReader(params.configFile), ::driverSettings)
            val scheduler = ThreadScheduler("LogicDevicesScheduler")
            val scenery = ScenesManager()

            configNode.devices.forEach {
                val driver = it.value.instantiateDevice(it.key, scheduler, scenery)
                if (driver != null) {
                    manager.addDriver(Device.Id(it.key), driver)
                }
            }

            scenery.bind("scenery", manager)
        }
    }
}

private fun DeviceNode.instantiateDevice(
        deviceName: String,
        scheduler: Scheduler,
        scenery: ScenesManager): DeviceDriver? {

    return when (Drivers.valueOf(driver)) {
        Drivers.SWITCH -> Switch()
        Drivers.BUTTON -> Button(scheduler)
        Drivers.PID_THERMOSTAT -> PidRegulatedThermostat(scheduler, settings as PidRegulatedThermostat.Settings)
        Drivers.SLOW_PWM -> SlowPwm(scheduler, settings as SlowPwm.Settings)
        Drivers.TEST_OUTPUT -> TestOutput()
        Drivers.TEST_INPUT -> TestInput(ConsoleLog(deviceName))
        Drivers.SCENE -> {
            addScene(deviceName, scenery, settings as ScenesManager.Settings)
            return null
        }
    }
}

fun addScene(name: String, scenery: ScenesManager, settings: ScenesManager.Settings) {
    val channels = settings.channels.map {
        if (it.activeFrom != 0f || it.activeTo != 1f) {
            if (it.factor == 1f) {
                Scene.Channel(it.input, RegionMapper(it.activeFrom, it.activeTo))
            } else {
                throw IllegalArgumentException("Mixed mappers are not supported")
            }
        } else {
            Scene.Channel(it.input, FactorMapper(it.factor))
        }
    }

    scenery.registerScene(name, channels)
}

enum class Drivers(val settingsClass: KClass<*>?) {
    SWITCH(null),
    BUTTON(null),
    PID_THERMOSTAT(PidRegulatedThermostat.Settings::class),
    SLOW_PWM(SlowPwm.Settings::class),
    TEST_INPUT(null),
    TEST_OUTPUT(null),
    SCENE(ScenesManager.Settings::class)
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