package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

class Splitter : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        val input = visitor.declareInput(
                "input",
                Types.BOOLEAN,
                Endpoint.Retention.NOT_RETAINED
        )
        val outputTrue = visitor.declareOutput(
                "true",
                Types.BOOLEAN,
                Endpoint.Retention.NOT_RETAINED
        )
        val outputFalse = visitor.declareOutput(
                "false",
                Types.BOOLEAN,
                Endpoint.Retention.NOT_RETAINED
        )

        input.observe {
            if (it) {
                outputTrue.set(true)
            } else {
                outputFalse.set(false)
            }
        }
    }
}