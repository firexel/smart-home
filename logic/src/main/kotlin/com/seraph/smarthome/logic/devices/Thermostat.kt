package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

class Thermostat : DeviceDriver {

    private val targetTemp = 30f
    private val allowedDelta = 0.3f

    override fun bind(visitor: DeviceDriver.Visitor) {
        val sensorInput = visitor.declareInput("from_sensor", Types.FLOAT, Endpoint.Retention.NOT_RETAINED)
        val switchOutput = visitor.declareOutput("to_switch", Types.BOOLEAN, Endpoint.Retention.NOT_RETAINED)

        sensorInput.observe { value ->
            when {
                value < targetTemp - allowedDelta -> switchOutput.set(true)
                value > targetTemp -> switchOutput.set(false)
            }
        }
    }
}