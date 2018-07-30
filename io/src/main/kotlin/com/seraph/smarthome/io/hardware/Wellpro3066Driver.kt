package com.seraph.smarthome.io.hardware

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import java.io.IOException

/**
 * Created by aleksandr.naumov on 06.05.18.
 */
class Wellpro3066Driver(
        private val moduleIndex: Byte,
        private val scheduler: Scheduler)
    : DeviceDriver {

    private val sensorsTotal = 8
    private val sensorsUpdatePeriodMs = 1000L

    override fun configure(visitor: DeviceDriver.Visitor) {
        val outputs = declareOutputs(visitor)
        requestData(outputs)
    }

    private fun declareOutputs(visitor: DeviceDriver.Visitor): List<SingleSensorOutputs> {
        return (0 until sensorsTotal)
                .map { index ->
                    with(visitor.declareInnerDevice("temp_sensor_$index")) {
                        val value = declareValueOutput(this, index)
                        val online = declareOnlineOutput(this, index)
                        SingleSensorOutputs(value, online)
                    }
                }
    }

    private fun declareOnlineOutput(visitor: DeviceDriver.Visitor, index: Int): DeviceDriver.Output<Boolean> {
        return visitor.declareOutput(
                "online",
                Types.BOOLEAN,
                Endpoint.Retention.RETAINED
        )
    }

    private fun declareValueOutput(visitor: DeviceDriver.Visitor, index: Int): DeviceDriver.Output<Float> {
        return visitor.declareOutput(
                "value",
                Types.FLOAT,
                Endpoint.Retention.RETAINED
        )
    }

    private fun requestData(outputs: List<SingleSensorOutputs>, delay: Long = 0) {
        scheduler.post(ReadRegisterCommand(moduleIndex, sensorsTotal), delay) { values ->
            outputs.forEachIndexed { index, output ->
                output.online.set(values[index].isPluggedIn)
                output.value.set(values[index].tempCelsius)
            }
            requestData(outputs, sensorsUpdatePeriodMs)
        }
    }

    private class ReadRegisterCommand(moduleIndex: Byte, private val sensorsTotal: Int)
        : ModbusRtuCommand<List<TempSensorState>>(moduleIndex, 0x03) {

        override fun writeRequestBody(output: BinaryOutputStream) = with(output) {
            write(0.toShort())
            write((sensorsTotal.toShort()), Endianness.MSB_FIRST)
        }

        override fun readResponseBody(input: BinaryInputStream): List<TempSensorState> {
            if (input.readByte() != (sensorsTotal * 2).toByte()) {
                throw IOException("Unexpected bytes length")
            }
            return (0 until sensorsTotal).map {
                val tempRaw = input.readUshort(Endianness.MSB_FIRST)
                when {
                    tempRaw == 0xffff -> TempSensorState(false, 0f)
                    tempRaw > 10000 -> TempSensorState(true, -(tempRaw - 10000) / 10f)
                    else -> TempSensorState(true, tempRaw / 10f)
                }
            }
        }
    }

    private data class SingleSensorOutputs(
            val value: DeviceDriver.Output<Float>,
            val online: DeviceDriver.Output<Boolean>
    )

    private data class TempSensorState(
            val isPluggedIn: Boolean,
            val tempCelsius: Float = 0f
    )
}