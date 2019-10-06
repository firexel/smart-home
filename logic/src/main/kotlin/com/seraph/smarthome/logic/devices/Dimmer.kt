package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Control
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

/**
 * Created by aleksandr.naumov on 01.01.18.
 */

class Dimmer : DeviceDriver {

    private var state: Boolean = false

    override fun bind(visitor: DeviceDriver.Visitor) {

        val stateInput = visitor.declareInput(
                "binary_state_in",
                Types.BOOLEAN,
                Endpoint.Retention.RETAINED
        )

        val impulseInput = visitor.declareInput(
                "switch",
                Types.VOID,
                Endpoint.Retention.NOT_RETAINED
        )

        val analogOutput = visitor.declareOutput(
                "analog_state_out",
                Types.FLOAT,
                Endpoint.Retention.RETAINED
        )

        val binaryOutput = visitor.declareOutput(
                "binary_state_out",
                Types.BOOLEAN,
                Endpoint.Retention.RETAINED
        )

        visitor.declareButton("switch", Control.Priority.MAIN, impulseInput)
        visitor.declareIndicator("state_indicator", Control.Priority.MAIN, binaryOutput)

        fun switchState(newState: Boolean) {
            state = newState
            analogOutput.set(if (newState) 1f else 0f)
            binaryOutput.set(newState)
        }

        stateInput.observe {
            switchState(it)
        }

        impulseInput.observe {
            switchState(!state)
        }
    }
}
