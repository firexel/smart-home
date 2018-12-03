package com.seraph.smarthome.io.hardware

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.DeviceState
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Log

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
class Wellpro8028Driver(
        private val scheduler: Scheduler,
        private val moduleIndex: Byte,
        private val log: Log)
    : DeviceDriver {

    private val sensorsCount = 8
    private val actorsCount = 8
    private val sensorsRefreshRateMs = 1L

    private var stateOutput: DeviceDriver.Output<DeviceState>? = null

    override fun configure(visitor: DeviceDriver.Visitor) {
        visitor.declareOutputPolicy(DeviceDriver.OutputPolicy.ALWAYS_ALLOW)
        configureDeviceState(visitor)
        configureActors(visitor)
        val sensors = configureSensors(visitor)
        sendReadCommand(sensors)
    }

    private fun configureDeviceState(visitor: DeviceDriver.Visitor) {
        stateOutput = visitor.declareOutput("state", Types.DEVICE_STATE, Endpoint.Retention.NOT_RETAINED)
    }

    private fun configureActors(visitor: DeviceDriver.Visitor) {
        (0 until actorsCount)
                .map { visitor.declareInput("relay_$it", Types.BOOLEAN, Endpoint.Retention.NOT_RETAINED) }
                .forEachIndexed { index, actor ->
                    actor.observe { value ->
                        sendWriteCommand(index, value)
                    }
                }
    }

    private fun configureSensors(visitor: DeviceDriver.Visitor): List<DeviceDriver.Output<Boolean>> {
        return (0 until sensorsCount)
                .map { visitor.declareOutput("switch_$it", Types.BOOLEAN, Endpoint.Retention.RETAINED) }
    }

    private fun sendReadCommand(sensors: List<DeviceDriver.Output<Boolean>>) {
        scheduler.post(ReadSensorsStateCommand(moduleIndex), sensorsRefreshRateMs) { state ->
            try {
                sensors.forEachIndexed { index, sensor ->
                    sensor.set(state.data[index])
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
}