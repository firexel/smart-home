package com.seraph.smarthome.device.testing

import com.seraph.smarthome.device.DeviceDriver

class MockInput<T> : DeviceDriver.Input<T> {

    private var observer: ((T) -> Unit)? = null

    override fun observe(observer: (T) -> Unit) {
        this.observer = observer
    }

    fun post(value: T) {
        observer!!.invoke(value)
    }
}