package com.seraph.smarthome.logic.devices

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.threading.Scheduler
import java.lang.Float.max
import java.lang.Float.min

class PidRegulatedThermostat(
        private val scheduler: Scheduler,
        private val settings: Settings)
    : DeviceDriver {

    private var insideTemp = Float.NaN
    private var setPoint = Float.NaN
    private var state = RegulatorState(cumulativeError = 0f, lastError = 0f, power = 0f)

    override fun bind(visitor: DeviceDriver.Visitor) {

        visitor.declareInput("inside", Types.FLOAT)
                .setUserInteraction(Endpoint.Interaction.USER_READONLY)
                .waitForDataBeforeOutput()
                .observe { insideTemp = it }

        visitor.declareInput("setpoint", Types.FLOAT)
                .setUserInteraction(Endpoint.Interaction.MAIN)
                .waitForDataBeforeOutput()
                .observe { setPoint = it }

        val powerOutput = visitor
                .declareOutput("power", Types.FLOAT)
                .setUserInteraction(Endpoint.Interaction.USER_READONLY)

        visitor.onOperational { cycle(powerOutput) }
    }

    private fun cycle(powerOutput: DeviceDriver.Output<Float>) {
        if (!insideTemp.isNaN() && !setPoint.isNaN()) {
            state = update(state)
            powerOutput.set(state.power)
        }
        scheduler.schedulePeriodically(settings.cycleLengthMs) { cycle(powerOutput) }
    }

    private fun update(state: RegulatorState): RegulatorState {

        val t = settings.cycleLengthMs / 1000f
        val kPd = settings.kP
        val kId = settings.kP * settings.kI * t
        val kDd = settings.kP * settings.kD / t

        val error = setPoint - insideTemp

        val pp = kPd * error
        val pi = kId * (error + state.cumulativeError)
        val pd = kDd * (error - state.lastError)

        return RegulatorState(
                cumulativeError = (state.cumulativeError + error).truncate(-1f / kId, 1 / kId),
                lastError = error,
                power = (pp + pi + pd).truncate(0f, 1f)
        )
    }

    private fun Float.truncate(minIncl: Float, maxIncl: Float) = max(min(this, maxIncl), minIncl)

    data class RegulatorState(
            val cumulativeError: Float,
            val lastError: Float,
            val power: Float
    )

    data class Settings(
            @SerializedName("cycle_length_ms")
            val cycleLengthMs: Long = 5000L,

            @SerializedName("k_p")
            val kP: Float = 1f / 15,

            @SerializedName("k_i")
            val kI: Float = 1f / 1000,

            @SerializedName("k_d")
            val kD: Float = 1f / 400,

            @SerializedName("default_temp")
            val defaultTemp: Float = 22f
    )
}