package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log

class TestInput(val log: Log) : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareInput("input", Types.INTEGER)
                .observe {
                    log.i("Got input $it")
                }
    }
}