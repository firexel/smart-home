package com.seraph.smarthome.io

import com.seraph.smarthome.io.hardware.ComPortConnection
import com.seraph.smarthome.io.hardware.Wellpro8028Device
import com.seraph.smarthome.transport.MqttBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.NoLog
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val params = CommandLineParams(ArgParser(argv))
            val settings = ComPortConnection.Settings(baudRate = 19200)
            val log = ConsoleLog()
            val connection = ComPortConnection(params.portName, settings, NoLog())
            val device = Wellpro8028Device(connection, params.deviceIndex.toByte())
            val broker = MqttBroker(params.brokerAddress, "I/O Service", log)
            DeviceServer(device, "io_device", "I/O Service Outputs")
                    .serve(broker)
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")

    val portName by parser.storing("-p", "--port", help = "system name of port to connect with PLC")
            .default("tty.usbserial")

    val deviceIndex: Int by parser.storing("--module_index", help = "index of the device on bus", transform = String::toIntMy)
            .default(1)
}

fun String.toIntMy() = toInt()