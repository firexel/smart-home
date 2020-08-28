package com.seraph.smarthome.io.hardware

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.InvalidData
import com.seraph.smarthome.device.validate
import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
class Wellpro8026Driver(
        private val scheduler: Scheduler,
        settings: Settings,
        private val log: Log)
    : DeviceDriver {

    private val sensorsRefreshRateMs = 1L
    private val sensorsConnected: Map<Int, String>
    private val moduleIndex: Byte = settings.addressAtBus

    private var stateOutput: DeviceDriver.Output<DeviceState>? = null

    init {
        settings.validate {
            val ioPattern = Regex("D[I]_((0[1-9])|(1[0-6]))")
            connections.keys.filter { !it.matches(ioPattern) }
                    .map { InvalidData(it, "Should match $ioPattern") }
        }

        sensorsConnected = settings.connections.keys
                .map { it.takeLast(2).toInt(10) - 1 to settings.connections.getValue(it) }
                .toMap()
    }

    override fun bind(visitor: DeviceDriver.Visitor) {
        configureDeviceState(visitor)
        val sensors = declareSensorEndpoints(visitor)
        sendReadCommand(sensors)
    }

    private fun configureDeviceState(visitor: DeviceDriver.Visitor) {
        stateOutput = visitor.declareOutput("state", Types.DEVICE_STATE)
    }

    private fun declareSensorEndpoints(visitor: DeviceDriver.Visitor): Map<Int, DeviceDriver.Output<Boolean>> {
        return sensorsConnected
                .map { it.key to visitor.declareOutput(it.value, Types.BOOLEAN) }
                .toMap()
    }

    private fun sendReadCommand(sensors: Map<Int, DeviceDriver.Output<Boolean>>) {
        scheduler.post(ReadSensorsStateCommand(moduleIndex), sensorsRefreshRateMs) { state ->
            try {
                state.data.forEachIndexed { index, sensorState ->
                    sensors[index]?.set(sensorState)
                }
            } catch (ex: Bus.CommunicationException) {
                log.w("Cannot read sensors state")
            }
            sendReadCommand(sensors)
        }
    }

    class ReadSensorsStateCommand(moduleIndex: Byte)
        : ModbusRtuCommand<BooleanArray>(moduleIndex, 0x02) {

        override fun writeRequestBody(output: BinaryOutputStream) = with(output) {
            write(0.toShort())
            write(0.toByte())
            write(0x10.toByte())
        }

        override fun readResponseBody(input: BinaryInputStream): BooleanArray {
            input.read()
            return input.readByteAsBits() + input.readByteAsBits()
        }
    }

    data class Settings(
            @SerializedName("address_at_bus")
            val addressAtBus: Byte,
            val connections: Map<String, String>
    )
}