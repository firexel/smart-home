package com.seraph.smarthome.io

import com.google.gson.Gson
import com.seraph.smarthome.model.*


/**
 * Created by aleksandr.naumov on 27.12.17.
 */
class DeviceServer(
        private val device: IoDevice,
        private val id: String,
        private val name: String,
        private val log: Log) {

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
        broker.publish(Topics.structure(descriptor.id), Gson().toJson(descriptor))
        for (i in 0 until device.outputsTotal) {
            broker.subscribe(Topics.input(descriptor.id, inputId(i))) { _, data ->
                val state = when (data) {
                    "true" -> true
                    "false" -> false
                    else -> {
                        log.w("Unknown incoming value $data")
                        return@subscribe
                    }
                }
                device.setOutputState(i, state)
            }
        }
    }
}