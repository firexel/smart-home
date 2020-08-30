package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types

class TestOutput : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        val output = visitor.declareOutput("output", Types.INTEGER)
        visitor.onOperational {
            var counter = 0
            Thread {
                while (true) {
                    output.set(counter)
                    counter++
                    Thread.sleep(1000)
                }
            }.start()
        }
    }
}