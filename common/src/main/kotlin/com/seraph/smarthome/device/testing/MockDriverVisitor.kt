package com.seraph.smarthome.device.testing

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint

/**
 * Created by aleksandr.naumov on 08.05.18.
 */
class MockDriverVisitor : DeviceDriver.Visitor {

    val outputs = mutableMapOf<String, MockOutput<*>>()
    val inners = mutableMapOf<String, MockDriverVisitor>()
    val inputs = mutableMapOf<String, MockInput<*>>()

    override fun declareInnerDevice(id: String): DeviceDriver.Visitor {
        val visitor = MockDriverVisitor()
        inners.put(id, visitor)
        return visitor
    }

    override fun <T> declareInput(id: String, type: Endpoint.Type<T>): DeviceDriver.Input<T> {
        val input = MockInput<T>()
        inputs.put(id, input)
        return input
    }

    override fun <T> declareOutput(id: String, type: Endpoint.Type<T>): DeviceDriver.Output<T> {
        val output = MockOutput<T>()
        outputs.put(id, output)
        return output
    }

    override fun onOperational(operation: () -> Unit) {
        TODO("not implemented")
    }
}

