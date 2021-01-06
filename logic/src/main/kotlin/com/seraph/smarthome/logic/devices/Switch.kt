package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

/**
 * Created by aleksandr.naumov on 01.01.18.
 */

class Switch : DeviceDriver {

    private var state: Boolean = false

    override fun bind(visitor: DeviceDriver.Visitor) {
        val stateInput = visitor.declareInput("state_in", Types.BOOLEAN)

        val impulseInput = visitor.declareInput("switch", Types.ACTION)
                .setDataKind(Endpoint.DataKind.EVENT)
                .setUserInteraction(Endpoint.Interaction.MAIN)

        val overridenOutput = visitor.declareOutput("state_out", Types.BOOLEAN)
                .setUserInteraction(Endpoint.Interaction.MAIN)

        fun switchState(newState: Boolean) {
            state = newState
            overridenOutput.set(newState)
        }

        stateInput.observe {
            switchState(it)
        }

        impulseInput.observe {
            switchState(!state)
        }
    }
}
