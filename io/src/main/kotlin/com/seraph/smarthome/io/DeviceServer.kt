package com.seraph.smarthome.io

import com.seraph.smarthome.model.Device
import com.seraph.smarthome.model.Endpoint
import com.seraph.smarthome.transport.BooleanConverter
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topics


/**
 * Created by aleksandr.naumov on 27.12.17.
 */
class DeviceServer(
        private val device: IoDevice,
        private val id: String,
        private val name: String) {

    private val descriptor = createOutputDeviceDescriptor()

    private fun createOutputDeviceDescriptor(): Device {
        if (device.outputsTotal <= 0) {
            throw IllegalArgumentException("Device without outputs given")
        }

        return Device(
                id = Device.Id(id),
                name = name,
                inputs = (0..(device.outputsTotal - 1)).map {
                    Endpoint(
                            inputId(it),
                            "Input #$it",
                            Endpoint.Type.BOOLEAN
                    )
                }
        )
    }

    private fun inputId(it: Int) = Endpoint.Id("input_$it")

    fun serve(broker: Broker) {
        Topics.structure(descriptor.id).publish(broker, descriptor)
        for (i in 0 until device.outputsTotal) {
            Topics.input(descriptor.id, inputId(i)).typed(BooleanConverter()).subscribe(broker) {
                device.setOutputState(i, it)
            }
        }
    }
}