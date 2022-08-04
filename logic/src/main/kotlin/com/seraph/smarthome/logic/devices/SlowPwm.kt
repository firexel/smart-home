package com.seraph.smarthome.logic.devices

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.threading.Scheduler
import java.util.concurrent.atomic.AtomicReference

class SlowPwm(
        private val scheduler: Scheduler,
        private val settings: Settings)
    : DeviceDriver {

    private val power = AtomicReference<Float>(0f)

    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareInput("power", Types.FLOAT)
                .waitForDataBeforeOutput()
                .observe { power.set(it) }

        val output = visitor.declareOutput("switch", Types.BOOLEAN)

        visitor.onOperational { cycle(output) }
    }

    private fun cycle(output: DeviceDriver.Output<Boolean>) {
        val power = power.get()
        val onTime = (settings.cycleLengthMs * power).toLong()
        when {
            // power fully on
            onTime >= settings.cycleLengthMs - settings.minStateTimeMs -> {
                output.set(true)
                scheduler.scheduleOnce(settings.cycleLengthMs) { cycle(output) }
            }
            // power partially on
            onTime > settings.minStateTimeMs -> {
                output.set(false)
                scheduler.scheduleOnce(settings.cycleLengthMs - onTime) {
                    output.set(true)
                    scheduler.scheduleOnce(onTime) { cycle(output) }
                }
            }
            // power fully off
            else -> {
                output.set(false)
                scheduler.scheduleOnce(settings.cycleLengthMs) { cycle(output) }
            }
        }
    }

    data class Settings(
            @SerializedName("cycle_length_ms")
            val cycleLengthMs: Long = 2000L,

            @SerializedName("min_state_time_ms")
            val minStateTimeMs: Long = 100L
    )
}