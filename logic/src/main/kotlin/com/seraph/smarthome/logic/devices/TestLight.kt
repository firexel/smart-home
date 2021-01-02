package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.domain.Units

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
                .setUnits(Units.PERCENTS_0_1)

        outputBoolean = visitor.declareOutput("output_bool", Types.BOOLEAN)
                .setUnits(Units.ON_OFF)

        visitor.declareOutput("output_action", Types.ACTION)

        visitor.declareInput("toggle", Types.ACTION)
                .observe { power = 1f.takeIf { power < 0.001f } ?: 0f }

        visitor.declareInput("on", Types.ACTION)
                .observe { power = 1f }

        visitor.declareInput("off", Types.ACTION)
                .observe { power = 0f }

        visitor.declareInput("input_float", Types.FLOAT)
                .setUnits(Units.PERCENTS_0_1)
                .observe { power = it }

        visitor.declareInput("input_bool", Types.BOOLEAN)
                .setUnits(Units.ON_OFF)
                .observe { input -> power = 1f.takeIf { input } ?: 0f }
    }
}