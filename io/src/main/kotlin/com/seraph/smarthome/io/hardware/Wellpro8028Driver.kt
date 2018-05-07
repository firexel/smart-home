package com.seraph.smarthome.io.hardware

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
class Wellpro8028Driver(
        private val scheduler: Scheduler,
        private val moduleIndex: Byte)
    : DeviceDriver {

    private val sensorsCount = 8
    private val actorsCount = 8
    private val sensorsRefreshRateMs = 1L

    private var sensorsState = BooleanArray(sensorsCount)

    override fun configure(visitor: DeviceDriver.Visitor) {
        configureActors(visitor)
        val sensors = configureSensors(visitor)
        sendReadCommand(sensors)
    }

    private fun configureActors(visitor: DeviceDriver.Visitor) {
        (0 until actorsCount)
                .map { visitor.declareInput("relay_$it", Types.BOOLEAN, Endpoint.Retention.RETAINED) }
                .forEachIndexed { index, actor ->
                    actor.observe { value ->
                        sendWriteCommand(index, value)
                    }
                }
    }

    private fun configureSensors(visitor: DeviceDriver.Visitor): List<DeviceDriver.Output<Boolean>> {
        val sensors = (0 until sensorsCount)
                .map { visitor.declareOutput("sensor_$it", Types.BOOLEAN, Endpoint.Retention.RETAINED) }

        sensors.forEachIndexed { index, output ->
            output.use {
                synchronized(this@Wellpro8028Driver) {
                    sensorsState[index]
                }
            }
        }

        return sensors
    }

    private fun sendReadCommand(sensors: List<DeviceDriver.Output<Boolean>>) {
        scheduler.post(ReadSensorsStateCommand(moduleIndex), sensorsRefreshRateMs) { sensorsState ->
            synchronized(this@Wellpro8028Driver) {
                this.sensorsState = sensorsState
            }
            sensors.forEach { it.invalidate() }
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
            input.skip(1)
            return input.readByteAsBits()
        }
    }
}