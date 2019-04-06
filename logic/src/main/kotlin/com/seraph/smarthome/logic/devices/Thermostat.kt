package com.seraph.smarthome.logic.devices

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

class Thermostat(private val settings: Settings) : DeviceDriver {

    override fun bind(visitor: DeviceDriver.Visitor) {
        val sensorInput = visitor.declareInput("from_sensor", Types.FLOAT, Endpoint.Retention.NOT_RETAINED)
        val switchOutput = visitor.declareOutput("to_switch", Types.BOOLEAN, Endpoint.Retention.NOT_RETAINED)

        sensorInput.observe { value ->
            when {
                value < settings.targetTemp - settings.allowedDelta -> switchOutput.set(true)
                value > settings.targetTemp -> switchOutput.set(false)
            }
        }
    }

    data class Settings(
            @SerializedName("target_temp")
            val targetTemp: Float,

            @SerializedName("allowed_delta")
            val allowedDelta: Float = 0.3f
    )
}