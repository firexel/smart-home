package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.logic.VirtualDevice

class VirtualSwitch : VirtualDevice {

    private var isOn = false

    override fun configure(visitor: VirtualDevice.Visitor) {
        val output = visitor.declareBoolOutput("on_off_output", "On/Off")
        val indicator = visitor.declareIndicator("on_off_indicator", VirtualDevice.Purpose.MAIN)
        val toggle = visitor.declareAction("toggle_action", VirtualDevice.Purpose.MAIN)

        output.use { isOn }
        indicator.use { isOn }
        toggle.observe {
            isOn = !isOn
            output.invalidate()
            indicator.invalidate()
        }
    }
}