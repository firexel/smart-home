package com.seraph.smarthome.io

import com.google.gson.Gson
import com.seraph.smarthome.model.*
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
            val broker = MqttBroker(params.brokerAddress, "SMIO", ConsoleLog())
            DeviceWithOutputs(broker).serve()
            DeviceWithInputs(broker, ConsoleLog()).serve()
        }
    }
}

class DeviceWithInputs(private val broker: Broker, private val log: Log) {
    private val device: Device
    private val input: Endpoint
    private val property: Endpoint

    init {
        val inputId = Endpoint.Id("integer_input")
        input = Endpoint(inputId, "Integer input", true, Endpoint.Type.INTEGER)

        val propertyId = Endpoint.Id("bool_property")
        property = Endpoint(inputId, "Bool property", true, Endpoint.Type.BOOLEAN)

        val id = Device.Id("device_with_input")
        device = Device(id, "Device with input", listOf(input), emptyList(), listOf(property))
        broker.publish(Topics.structure(device.id), Gson().toJson(device))
    }

    fun serve() {
        val times = mutableListOf<Long>()
        broker.subscribe(Topics.output(device.id, input.id)) { _, data ->
            val sendTime = data.toLong()
            val timeDifference = System.nanoTime() - sendTime
            times.add(timeDifference)
            log.i("Message traveled for $timeDifference, avg: ${times.takeLast(15).average()}")
        }
    }
}

class DeviceWithOutputs(private val broker: Broker) {
    private val device: Device
    private val output: Endpoint

    init {
        val outputId = Endpoint.Id("integer_output")
        output = Endpoint(outputId, "Integer output", true, Endpoint.Type.INTEGER)
        val id = Device.Id("device_with_output")
        device = Device(id, "Device with output", emptyList(), listOf(output), emptyList())
        broker.publish(Topics.structure(device.id), Gson().toJson(device))
    }

    fun serve() {
        Thread {
            while (true) {
                broker.publish(Topics.structure(device.id), Gson().toJson(device))

                //broker.publish(Topics.output(device.id, output.id), System.nanoTime().toString())
                Thread.sleep(5000)
            }
        }.start()
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
            .default("tcp://localhost:1883")
}