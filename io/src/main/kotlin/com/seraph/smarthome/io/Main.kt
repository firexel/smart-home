package com.seraph.smarthome.io

import com.fazecast.jSerialComm.SerialPort
import com.google.gson.Gson
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.io.hardware.ComPortConnection
import com.seraph.smarthome.io.hardware.Wellpro8028Device
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.NoLog
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
            val log = ConsoleLog()
            val broker = StatefulMqttBroker(params.brokerAddress, "I/O Service", log.copy("Broker"))
            val network = MqttNetwork(broker, log.copy("Network"))
            val config = Gson().fromJson(FileReader(params.configFile), Config::class.java)
            config.buses.forEach { bus ->
                val settings = bus.settings.asPortSettings()
                val connection = ComPortConnection(bus.settings.path, settings, log.copy("Com"))
                bus.modules.forEach { module ->
                    val device = module.asDeviceInstance(connection)
                    DeviceServer(network, device, "${bus.name}:${module.name}").serve()
                }
            }
        }
    }
}

private fun ModbusModule.asDeviceInstance(connection: ComPortConnection): IoDevice = when (model) {
    ModbusDeviceModel.WELLPRO_8028 -> Wellpro8028Device(connection, index.toByte())
}

private fun PortSettings.asPortSettings() = ComPortConnection.Settings(
        baudRate, parity.asConnectionParityIndex(), dataBits, stopBits
)

private fun Parity.asConnectionParityIndex() = when (this) {
    Parity.NO -> SerialPort.NO_PARITY
    Parity.ODD -> SerialPort.ODD_PARITY
    Parity.EVEN -> SerialPort.EVEN_PARITY
    Parity.MARK -> SerialPort.MARK_PARITY
    Parity.SPACE -> SerialPort.SPACE_PARITY
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