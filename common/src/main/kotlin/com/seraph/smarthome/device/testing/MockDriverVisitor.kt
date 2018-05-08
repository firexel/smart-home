package com.seraph.smarthome.device.testing

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Control
import com.seraph.smarthome.domain.Endpoint

/**
 * Created by aleksandr.naumov on 08.05.18.
 */
class MockDriverVisitor : DeviceDriver.Visitor {

    val outputs = mutableMapOf<String, MockOutput<*>>()
    val innerDevices = mutableMapOf<String, MockDriverVisitor>()

    override fun declareInnerDevice(id: String): DeviceDriver.Visitor {
        val visitor = MockDriverVisitor()
        innerDevices.put(id, visitor)
        return visitor
    }

    override fun <T> declareInput(id: String, type: Endpoint.Type<T>, retention: Endpoint.Retention): DeviceDriver.Input<T> {
        return MockInput()
    }

    override fun <T> declareOutput(id: String, type: Endpoint.Type<T>, retention: Endpoint.Retention): DeviceDriver.Output<T> {
        val output = MockOutput<T>()
        outputs.put(id, output)
        return output
    }

    override fun declareIndicator(id: String, priority: Control.Priority, source: DeviceDriver.Output<Boolean>) {
        TODO("not implemented")
    }

    override fun declareButton(id: String, priority: Control.Priority, input: DeviceDriver.Input<Unit>, alert: String) {
        TODO("not implemented")
    }
}

