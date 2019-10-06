package com.seraph.smarthome.io.hardware.dmx.fixture

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.io.hardware.dmx.UniverseController

class StandaloneLightFixture(
        private val interpolator: Interpolator
) : UniverseController.Fixture, DeviceDriver {

    override var value: Float = interpolator.progress(0)

    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareInput("brightness", Types.FLOAT, Endpoint.Retention.RETAINED).observe {
            interpolator.setTarget(it)
        }
    }

    override fun update(nanosPassed: Long) {
        value = interpolator.progress(nanosPassed)
    }

    interface Interpolator {
        fun setTarget(target: Float)
        fun progress(nanosPassed: Long): Float
        val isStable: Boolean
    }
}