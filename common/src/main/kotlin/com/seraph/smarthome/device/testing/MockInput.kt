package com.seraph.smarthome.device.testing

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Units

class MockInput<T> : DeviceDriver.Input<T> {

    private var observer: ((T) -> Unit)? = null

    override fun observe(observer: (T) -> Unit) {
        this.observer = observer
    }

    fun post(value: T) {
        observer!!.invoke(value)
    }

    override fun setDataKind(dataKind: Endpoint.DataKind): DeviceDriver.Input<T> {
        TODO("not implemented")
    }

    override fun setUserInteraction(interaction: Endpoint.Interaction): DeviceDriver.Input<T> {
        TODO("not implemented")
    }

    override fun setUnits(units: Units): DeviceDriver.Input<T> {
        TODO("not implemented")
    }

    override fun waitForDataBeforeOutput(): DeviceDriver.Input<T> {
        TODO("not implemented")
    }
}