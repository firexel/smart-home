package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log

class TestInput(val log: Log) : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareInput("int_input", Types.INTEGER)
                .observe {
                    log.i("Got int input $it")
                }

        visitor.declareInput("bool_input", Types.BOOLEAN)
                .observe {
                    log.i("Got bool input $it")
                }

        visitor.declareInput("float_input", Types.FLOAT)
                .observe {
                    log.i("Got float input $it")
                }
    }
}