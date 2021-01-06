package com.seraph.smarthome.device.testing

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Units

/**
 * Created by aleksandr.naumov on 07.05.18.
 */
class MockOutput<T> : DeviceDriver.Output<T> {

    private var invalidateCount = 0
    private var source: () -> T = { throw IllegalStateException("Value not set yet") }

    val value: T
        get() = source()

    val timesInvalidated: Int
        get() = invalidateCount

    override fun set(update: T) {
        source = { update }
        invalidateCount++
    }

    override fun setDataKind(dataKind: Endpoint.DataKind): DeviceDriver.Output<T> {
        TODO("not implemented")
    }

    override fun setUserInteraction(interaction: Endpoint.Interaction): DeviceDriver.Output<T> {
        TODO("not implemented")
    }

    override fun setUnits(units: Units): DeviceDriver.Output<T> {
        TODO("not implemented")
    }
}