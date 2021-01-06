package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types

class TestOutput : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        val outputInt = visitor.declareOutput("output_int", Types.INTEGER)
        val outputFloat = visitor.declareOutput("output_float", Types.FLOAT)
        val outputBoolean = visitor.declareOutput("output_boolean", Types.BOOLEAN)
        val outputAction = visitor.declareOutput("output_action", Types.ACTION)

        visitor.onOperational {
            var counter = 0
            Thread {
                while (true) {
                    outputInt.set(counter)
                    outputFloat.set(counter.toFloat())
                    outputBoolean.set(counter % 2 == 1)
                    outputAction.set(Types.newActionId())
                    counter++
                    Thread.sleep(1000)
                }
            }.start()
        }
    }
}