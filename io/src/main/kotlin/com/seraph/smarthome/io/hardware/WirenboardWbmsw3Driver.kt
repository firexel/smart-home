package com.seraph.smarthome.io.hardware

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log

class WirenboardWbmsw3Driver(
        private val scheduler: Scheduler,
        private val settings: Settings,
        private val log: Log)
    : DeviceDriver {


    override fun bind(visitor: DeviceDriver.Visitor) {
        val busAddr = settings.addressAtBus

        visitor.declareOutput("temperature", Types.FLOAT)
                .beginReadings(ReadFloat16Cmd(busAddr, 0))

        visitor.declareOutput("humidity", Types.FLOAT)
                .beginReadings(ReadFloat16Cmd(busAddr, 1))

        visitor.declareOutput("illumination", Types.FLOAT)
                .beginReadings(ReadFloat32Cmd(busAddr, 9))

        visitor.declareOutput("co2", Types.INTEGER)
                .beginReadings(ReadInt16Cmd(busAddr, 8))

        visitor.declareOutput("aoc", Types.INTEGER)
                .beginReadings(ReadInt16Cmd(busAddr, 11))

    }

    private fun <T> DeviceDriver.Output<T>.beginReadings(cmd: Bus.Command<T>) {
        scheduler.post(cmd, 1000) {
            if (it.isSuccess) {
                this.set(it.data)
            }
            beginReadings(cmd)
        }
    }

    inner class ReadFloat16Cmd(addressAtBus: Byte, index: Int) : ReadInputRegisterCmd<Float>(addressAtBus, index) {
        override fun dealWithResponse(input: BinaryInputStream): Float {
            return input.readShort(Endianness.MSB_FIRST) / 10f
        }
    }

    inner class ReadInt16Cmd(addressAtBus: Byte, index: Int) : ReadInputRegisterCmd<Int>(addressAtBus, index) {
        override fun dealWithResponse(input: BinaryInputStream): Int {
            return input.readShort(Endianness.MSB_FIRST).toInt()
        }
    }

    inner class ReadFloat32Cmd(addressAtBus: Byte, index: Int) : ReadInputRegisterCmd<Float>(addressAtBus, index, 2) {
        override fun dealWithResponse(input: BinaryInputStream): Float {
            return input.readInt(Endianness.MSB_FIRST) / 100f
        }
    }

    data class Settings(
            @SerializedName("address_at_bus")
            val addressAtBus: Byte
    )
}