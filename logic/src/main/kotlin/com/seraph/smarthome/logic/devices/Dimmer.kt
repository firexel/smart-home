package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.domain.Units
import kotlin.math.roundToInt

class Dimmer(private val settings: Settings) : DeviceDriver {

    data class Settings(val onValue: Int, val offValue: Int)

    override fun bind(visitor: DeviceDriver.Visitor) {
        val outBool = visitor.declareOutput("key_out", Types.BOOLEAN)
            .setUnits(Units.ON_OFF)
            .setDataKind(Endpoint.DataKind.CURRENT)

        val outInt = visitor.declareOutput("int_out", Types.INTEGER)
            .setDataKind(Endpoint.DataKind.CURRENT)

        visitor.declareInput("key_in", Types.BOOLEAN)
            .setUnits(Units.ON_OFF)
            .setDataKind(Endpoint.DataKind.CURRENT)
            .observe {
                outBool.set(it)
                outInt.set(if (it) settings.onValue else settings.offValue)
            }

        visitor.declareInput("float_in", Types.FLOAT)
            .setUnits(Units.PERCENTS_0_1)
            .setDataKind(Endpoint.DataKind.CURRENT)
            .observe {
                when {
                    it <= 0.001f -> {
                        outBool.set(false)
                        outInt.set(settings.offValue)
                    }
                    it >= (1f - 0.001f) -> {
                        outBool.set(true)
                        outInt.set(settings.onValue)
                    }
                    else -> {
                        outBool.set(true)
                        outInt.set(toOutInt(it))
                    }
                }
            }
    }

    fun toOutInt(value: Float): Int {
        return ((settings.onValue + settings.offValue) * value).roundToInt()
    }
}