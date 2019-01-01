package com.seraph.smarthome.io

import com.fazecast.jSerialComm.SerialPort
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DeviceManager
import com.seraph.smarthome.device.DriverConfiguration
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
import kotlin.reflect.full.cast

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val params = CommandLineParams(ArgParser(argv))
            val log = ConsoleLog("IO").apply { i("Starting...") }
            val broker = StatefulMqttBroker(params.brokerAddress, "I/O Service", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val config = readConfig(FileReader(params.configFile))
            val manager = DeviceManager(network, Device.Id("io"), log = log.copy("Manager"))

            config.buses.forEach { (busId, busNode) ->
                val settings = busNode.settings.mapToSerialBusSettings()
                val busLog = log.copy(busId)
                val busDriver = SerialBus(busNode.settings.path, settings, busLog)
                val busScheduler = ConcurrentScheduler(busDriver)
                busNode.devices.forEach { (deviceId, deviceNode) ->
                    val driver = deviceNode.instantiateDriver(busScheduler, busLog.copy(deviceId))
                    manager.addDriver(Device.Id(busId, deviceId), driver)
                }
            }
        }
    }
}

private fun <S : Any> DeviceNode.instantiateConfig(classToCast: KClass<S>)
        : DriverConfiguration<S> {

    val map = this.connections.map { it.key to DriverConfiguration.Alias(it.value.names) }.toMap()
    return DriverConfiguration(classToCast.cast(settings), DriverConfiguration.Connections(map))
}

private fun DeviceNode.instantiateDriver(scheduler: Scheduler, log: Log): DeviceDriver = when (driver) {
    DriverNameNode.WELLPRO_8028 ->
        Wellpro8028Driver(scheduler, instantiateConfig(ModbusDeviceSettingsNode::class), log)

    DriverNameNode.WELLPRO_3066 ->
        Wellpro3066Driver(scheduler, instantiateConfig(ModbusDeviceSettingsNode::class).settings.addressAtBus, log)
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