package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Control
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

/**
 * Created by aleksandr.naumov on 01.01.18.
 */

class OverrideSwitchDriver : DeviceDriver {

    private var state: Boolean = false

    override fun configure(visitor: DeviceDriver.Visitor) {
        val stateInput = visitor.declareInput(
                "state",
                Types.BOOLEAN,
                Endpoint.Retention.RETAINED
        )

        val impulseInput = visitor.declareInput(
                "impulse",
                Types.VOID,
                Endpoint.Retention.NOT_RETAINED
        )

        val overridenOutput = visitor.declareOutput(
                "overriden",
                Types.BOOLEAN,
                Endpoint.Retention.RETAINED
        )

        visitor.declareButton("override", Control.Priority.MAIN, impulseInput)
        visitor.declareIndicator("state", Control.Priority.MAIN, overridenOutput)

        fun switchState(newState: Boolean) {
            if (newState != state) {
                state = newState
                overridenOutput.invalidate()
            }
        }

        stateInput.observe {
            switchState(it)
        }

        impulseInput.observe {
            switchState(!state)
        }

        overridenOutput.use { state }
    }
}
