package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.logic.VirtualDevice

/**
 * Created by aleksandr.naumov on 01.01.18.
 */

class VirtualOverrideSwitch : VirtualDevice {

    private var state: Boolean = false

    override fun configure(visitor: VirtualDevice.Visitor) {
        val otherSwitch = visitor.declareBoolInput("other_switch", "Other switch")
        val overridenOutput = visitor.declareBoolOutput("overriden_output", "Overriden output")
        val overrideAction = visitor.declareAction("override", VirtualDevice.Purpose.MAIN)
        val stateIndicator = visitor.declareIndicator("state", VirtualDevice.Purpose.MAIN)

        fun switchState(newState: Boolean) {
            if (newState != state) {
                state = newState
                stateIndicator.invalidate()
                overridenOutput.invalidate()
            }
        }

        otherSwitch.observe {
            switchState(it)
        }

        overrideAction.observe {
            switchState(!state)
        }

        stateIndicator.use { state }
        overridenOutput.use { state }
    }
}
