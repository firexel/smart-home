package com.seraph.smarthome.io

import com.fazecast.jSerialComm.SerialPort
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.io.hardware.*
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.Log
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
            val params = CommandLineParams(ArgParser(argv))
            val log = ConsoleLog("IO").apply { i("Starting witch commandline ${argv.toList()}...") }
            val broker = StatefulMqttBroker(params.brokerAddress, "I/TransformationVisitor Service", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val configNode = readConfig(FileReader(params.configFile), ::driverSettings)
            val manager = DriversManager(network, Device.Id("io"), log = log.copy("Manager"))

            configNode.buses.forEach { (busId, busNode) ->
                val settings = busNode.settings.mapToSerialBusSettings()
                val busLog = log.copy(busId)
                val busDriver = SerialBus(busNode.settings.path, settings, busLog)
                val busScheduler = ConcurrentScheduler(busDriver)
                busNode.devices.forEach { (deviceName, deviceNode) ->
                    val driver = deviceNode.instantiateDriver(busScheduler, busLog.copy(deviceName))
                    val deviceId = Device.Id(busId, deviceName)
                    manager.addDriver(deviceId, driver)
                }
            }
        }
    }
}

enum class Drivers(val settingsClass: KClass<*>) {
    WELLPRO_8028(Wellpro8028Driver.Settings::class),
    WELLPRO_3066(Wellpro3066Driver.Settings::class),
    WIRENBOARD_WBMSW3(WirenboardWbmsw3Driver.Settings::class)
}

private fun driverSettings(name: String): DriverInfo {
    return DriverInfo(Drivers.valueOf(name).settingsClass)
}

private fun DeviceNode.instantiateDriver(scheduler: Scheduler, log: Log): DeviceDriver {
    return when (Drivers.valueOf(driver)) {
        Drivers.WELLPRO_8028 -> {
            Wellpro8028Driver(scheduler, settings as Wellpro8028Driver.Settings, log)
        }

        Drivers.WELLPRO_3066 -> {
            Wellpro3066Driver(scheduler, settings as Wellpro3066Driver.Settings, log)
        }

        Drivers.WIRENBOARD_WBMSW3 -> {
            WirenboardWbmsw3Driver(scheduler, settings as WirenboardWbmsw3Driver.Settings, log)
        }
    }
}

private fun PortSettingsNode.mapToSerialBusSettings() = SerialBus.Settings(
        baudRate, parity.mapToConnectionParityIndex(), dataBits, stopBits
)

private fun ParityNode.mapToConnectionParityIndex() = when (this) {
    ParityNode.NO -> SerialPort.NO_PARITY
    ParityNode.ODD -> SerialPort.ODD_PARITY
    ParityNode.EVEN -> SerialPort.EVEN_PARITY
    ParityNode.MARK -> SerialPort.MARK_PARITY
    ParityNode.SPACE -> SerialPort.SPACE_PARITY
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")

    val configFile by parser.storing("-c", "--config", help = "path to config") {
        File(this)
    }.default(File("/etc/io.json")).addValidator {
        if (!value.exists()) {
            throw SystemExitException("Config not found at ${value.absoluteFile}", -1)
        }
    }
}