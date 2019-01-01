package com.seraph.smarthome.io

import com.fazecast.jSerialComm.SerialPort
import com.seraph.smarthome.device.DeviceManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import java.io.File
import java.io.FileReader

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

//            config.buses.forEach { bus ->
//                val settings = bus.settings.asSerialBusSettings()
//                val busDriver = SerialBusNode(bus.settings.path, settings, log.copy("${bus.name.capitalize()}Bus"))
//                val busScheduler = ConcurrentScheduler(busDriver)
//                bus.modules.forEach { location ->
//                    val id = Device.Id(bus.name, location.value.name)
//                    val device = location.value.asDriverInstance(location.key, busScheduler, log.copy("Device_$id"))
//                    manager.addDriver(id, device)
//                }
            }
        }
    }
//}
//
//private fun Module.asDriverInstance(index: Byte, scheduler: Scheduler, log: Log): DeviceDriver = when (model) {
//    DeviceDriverNameNode.WELLPRO_8028 -> Wellpro8028Driver(scheduler, index, log)
//    DeviceDriverNameNode.WELLPRO_3066 -> Wellpro3066Driver(scheduler, index, log)
//}
//
//private fun PortSettingsNode.asSerialBusSettings() = SerialBusNode.Settings(
//        baudRate, parity.asConnectionParityIndex(), dataBits, stopBits
//)

private fun ParityNode.asConnectionParityIndex() = when (this) {
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