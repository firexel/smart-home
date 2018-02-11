package com.seraph.smarthome.io

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.domain.Types
import java.lang.Integer.min


/**
 * Created by aleksandr.naumov on 27.12.17.
 */
class DeviceServer(
        private val network: Network,
        private val device: IoDevice,
        private val id: String) {

    companion object {
        val serviceName = "io"
    }

    private val inputEndpoints = (0 until device.relaysTotal).map {
        Endpoint(
                id = Endpoint.Id("input_$it"),
                type = Types.BOOLEAN,
                direction = Endpoint.Direction.INPUT,
                retention = Endpoint.Retention.RETAINED
        )
    }

    private val outputEndpoints = (0 until device.sensorsTotal).map {
        Endpoint(
                id = Endpoint.Id("output_$it"),
                type = Types.BOOLEAN,
                direction = Endpoint.Direction.OUTPUT,
                retention = Endpoint.Retention.RETAINED
        )
    }

    private val inputsDescriptor = createInputsDeviceDescriptor()
    private val outputsDescriptor = createOutputsDeviceDescriptor()

    private fun createInputsDeviceDescriptor(): Device {
        if (device.relaysTotal <= 0) {
            throw IllegalArgumentException("Device without outputs given")
        }

        return Device(
                id = Device.Id(listOf(serviceName, id, "inputs")),
                endpoints = inputEndpoints,
                controls = emptyList()
        )
    }

    private fun createOutputsDeviceDescriptor(): Device {
        if (device.sensorsTotal <= 0) {
            throw IllegalArgumentException("Device without inputs given")
        }

        return Device(
                id = Device.Id(listOf(serviceName, id, "outputs")),
                endpoints = outputEndpoints,
                controls = emptyList()
        )
    }

    fun serve() {
        network.publish(inputsDescriptor)
        network.publish(outputsDescriptor)

        inputEndpoints.forEachIndexed { index, endpoint ->
            network.subscribe(inputsDescriptor.id, endpoint) { _, _, data ->
                device.setRelayState(index, data)
            }
        }

        var initialState = device.getSensorsState()
        for (i in 0 until initialState.size) {
            publishOutput(i, initialState[i])
        }

        while (true) {
            val newState = device.getSensorsState()
            (0 until min(newState.size, initialState.size))
                    .filter { newState[it] != initialState[it] }
                    .forEach { publishOutput(it, newState[it]) }
            initialState = newState
            Thread.sleep(10)
        }
    }

    private fun publishOutput(sensorIndex: Int, sensorState: Boolean) {
        network.publish(outputsDescriptor.id, outputEndpoints[sensorIndex], sensorState)
    }
}