package com.seraph.smarthome.io.hardware.dmx.fixture

class LinearInterpolator(initial: Double) : StandaloneLightFixture.Interpolator {

    private val fractionPerSecond = 0.3

    private var value = initial
    private var target = initial

    override fun setTarget(target: Float) {
        this.target = Math.max(Math.min(target.toDouble(), 1.0), 0.0)
    }

    override fun progress(nanosPassed: Long): Float {
        val delta: Double = fractionPerSecond / 1e9 * nanosPassed
        when {
            Math.abs(value - target) <= delta -> value = target
            target - value > 0 -> value += delta // going up
            else -> value -= delta // going down
        }
        return value.toFloat()
    }

    override val isStable: Boolean
        get() = value == target
}