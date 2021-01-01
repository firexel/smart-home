package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types

class TestLight : DeviceDriver {

    private lateinit var outputFloat: DeviceDriver.Output<Float>
    private lateinit var outputBoolean: DeviceDriver.Output<Boolean>

    private var power = 0f
        set(value) {
            field = value
            outputFloat.set(value)
            outputBoolean.set(value > 0.01f)
        }

    override fun bind(visitor: DeviceDriver.Visitor) {

        outputFloat = visitor.declareOutput("output_float", Types.FLOAT)
        outputBoolean = visitor.declareOutput("output_bool", Types.BOOLEAN)
        visitor.declareOutput("output_action", Types.ACTION)

        visitor.declareInput("toggle", Types.ACTION)
                .observe {
                    power = if (power < 0.01f) {
                        1f
                    } else {
                        0f
                    }
                }

        visitor.declareInput("input_float", Types.FLOAT)
                .observe {
                    power = it
                }

        visitor.declareInput("input_bool", Types.BOOLEAN)
                .observe {
                    power = if (it) {
                        1f
                    } else {
                        0f
                    }
                }
    }
}