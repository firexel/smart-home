package com.seraph.smarthome.io.hardware

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.InvalidData
import com.seraph.smarthome.device.validate
import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
class Wellpro8028Driver(
        private val scheduler: Scheduler,
        settings: Settings,
        private val log: Log)
    : DeviceDriver {

    private val sensorsRefreshRateMs = 1L
    private val sensorsConnected: Map<Int, String>
    private val relaysConnected: Map<Int, String>
    private val moduleIndex: Byte = settings.addressAtBus

    private var stateOutput: DeviceDriver.Output<DeviceState>? = null

    init {
        settings.validate {
            val ioPattern = Regex("D[IO]_0[1-8]")
            connections.keys.filter { !it.matches(ioPattern) }
                    .map { InvalidData(it, "Should match $ioPattern") }
        }

        sensorsConnected = settings.connections.keys
                .filter { it.startsWith("DI") }
                .map { it.takeLast(1).toInt() - 1 to settings.connections.getValue(it) }
                .toMap()

        relaysConnected = settings.connections.keys
                .filter { it.startsWith("DO") }
                .map { it.takeLast(1).toInt() - 1 to settings.connections.getValue(it) }
                .toMap()
    }

    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareOutputPolicy(DeviceDriver.OutputPolicy.ALWAYS_ALLOW)
        configureDeviceState(visitor)
        declareRelayEndpoints(visitor)
        val sensors = declareSensorEndpoints(visitor)
        sendReadCommand(sensors)
    }

    private fun configureDeviceState(visitor: DeviceDriver.Visitor) {
        stateOutput = visitor.declareOutput("state", Types.DEVICE_STATE, Endpoint.Retention.NOT_RETAINED)
    }

    private fun declareRelayEndpoints(visitor: DeviceDriver.Visitor) {
        relaysConnected.forEach {
            visitor.declareInput(it.value, Types.BOOLEAN, Endpoint.Retention.NOT_RETAINED)
                    .observe { value -> sendWriteCommand(it.key, value) }
        }
    }

    private fun declareSensorEndpoints(visitor: DeviceDriver.Visitor): Map<Int, DeviceDriver.Output<Boolean>> {
        return sensorsConnected
                .map { it.key to visitor.declareOutput(it.value, Types.BOOLEAN, Endpoint.Retention.RETAINED) }
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

    private fun sendWriteCommand(index: Int, value: Boolean) {
        scheduler.post(WriteActorCommand(moduleIndex, index, value))
    }

    class WriteActorCommand(
            module: Byte,
            private val index: Int,
            private val enable: Boolean)
        : ModbusRtuCommand<Unit>(module, 0x05) {

        override fun writeRequestBody(output: BinaryOutputStream) = with(output) {
            write(0.toByte())
            write(index.toByte())
            write(if (enable) 0x00ff.toShort() else 0x0000.toShort())
        }

        override fun readResponseBody(input: BinaryInputStream) {
            input.skip(4)
        }
    }

    class ReadSensorsStateCommand(moduleIndex: Byte)
        : ModbusRtuCommand<BooleanArray>(moduleIndex, 0x02) {

        override fun writeRequestBody(output: BinaryOutputStream) = with(output) {
            write(0.toShort())
            write(0.toByte())
            write(0x08.toByte())
        }

        override fun readResponseBody(input: BinaryInputStream): BooleanArray {
            input.read()
            return input.readByteAsBits()
        }
    }

    data class Settings(
            @SerializedName("address_at_bus")
            val addressAtBus: Byte,
            val connections: Map<String, String>
    )
}