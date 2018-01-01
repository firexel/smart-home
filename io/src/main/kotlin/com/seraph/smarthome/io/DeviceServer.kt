package com.seraph.smarthome.io

import com.seraph.smarthome.model.Device
import com.seraph.smarthome.model.Endpoint
import com.seraph.smarthome.transport.BooleanConverter
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topics
import java.lang.Integer.min


/**
 * Created by aleksandr.naumov on 27.12.17.
 */
class DeviceServer(
        private val device: IoDevice,
        private val id: String,
        private val name: String) {

    private val inputsDescriptor = createInputsDeviceDescriptor()
    private val outputsDescriptor = createOutputsDeviceDescriptor()

    private fun createInputsDeviceDescriptor(): Device {
        if (device.relaysTotal <= 0) {
            throw IllegalArgumentException("Device without outputs given")
        }

        return Device(
                id = Device.Id("${id}_inputs"),
                name = name,
                inputs = (0 until device.relaysTotal).map {
                    Endpoint(
                            inputId(it),
                            "Input #$it",
                            Endpoint.Type.BOOLEAN
                    )
                }
        )
    }

    private fun createOutputsDeviceDescriptor(): Device {
        if (device.sensorsTotal <= 0) {
            throw IllegalArgumentException("Device without inputs given")
        }

        return Device(
                id = Device.Id("${id}_outputs"),
                name = name,
                outputs = (0 until device.sensorsTotal).map {
                    Endpoint(
                            outputId(it),
                            "Output #$it",
                            Endpoint.Type.BOOLEAN
                    )
                }
        )
    }

    private fun inputId(it: Int) = Endpoint.Id("input_$it")

    private fun outputId(it: Int) = Endpoint.Id("output_$it")

    fun serve(broker: Broker) {
        Topics.structure(inputsDescriptor.id).publish(broker, inputsDescriptor)
        Topics.structure(outputsDescriptor.id).publish(broker, outputsDescriptor)

        for (i in 0 until device.relaysTotal) {
            Topics.input(inputsDescriptor.id, inputId(i)).typed(BooleanConverter()).subscribe(broker) {
                device.setRelayState(i, it)
            }
        }

        var initialState = device.getSensorsState()
        for (i in 0 until initialState.size) {
            publishOutput(broker, i, initialState[i])
        }

        while (true) {
            val newState = device.getSensorsState()
            (0 until min(newState.size, initialState.size))
                    .filter { newState[it] != initialState[it] }
                    .forEach { publishOutput(broker, it, newState[it]) }
            initialState = newState
            Thread.sleep(10)
        }
    }

    private fun publishOutput(broker: Broker, sensorIndex: Int, sensorState: Boolean) {
        Topics.output(outputsDescriptor.id, outputId(sensorIndex))
                .typed(BooleanConverter())
                .publish(broker, sensorState)
    }
}