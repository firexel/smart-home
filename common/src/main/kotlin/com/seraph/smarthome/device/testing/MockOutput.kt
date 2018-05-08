package com.seraph.smarthome.device.testing

import com.seraph.smarthome.device.DeviceDriver

/**
 * Created by aleksandr.naumov on 07.05.18.
 */
class MockOutput<T> : DeviceDriver.Output<T> {

    private var invalidateCount = 0
    private var source: () -> T = { throw IllegalStateException("Source should be set") }

    val value: T
        get() = source()

    val timesInvalidated: Int
        get() = invalidateCount

    override fun use(source: () -> T) {
        this.source = source
    }

    override fun invalidate() {
        invalidateCount++
    }
}