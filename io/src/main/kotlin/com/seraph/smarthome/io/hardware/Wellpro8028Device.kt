package com.seraph.smarthome.io.hardware

import com.seraph.smarthome.io.IoDevice
import java.io.ByteArrayOutputStream
import kotlin.experimental.and

/**
 * Created by aleksandr.naumov on 24.12.17.
 */
class Wellpro8028Device(
        private val connection: Connection,
        private val moduleIndex: Byte)
    : IoDevice {

    override val sensorsTotal: Int
        get() = 8

    override val relaysTotal: Int
        get() = 8

    override fun getSensorsState(): BooleanArray {
        val response = Command(moduleIndex, 0x02)
                .append(0.toShort())
                .append(0.toByte())
                .append(0x08.toByte())
                .send(6)



        if (response.size == 6) {
            val inputs = BooleanArray(sensorsTotal)
            val state = response[3]
            var inputBit: Byte = 1
            for (i in 0 until sensorsTotal) {
                inputs[i] = (state and inputBit) > 0
                inputBit = inputBit.toInt().shl(1).toByte()
            }
            return inputs
        } else {
            throw IllegalArgumentException("Unknown response of ${response.size} bytes")
        }
    }

    override fun setRelayState(relayIndex: Int, enable: Boolean) {
        if (relayIndex >= relaysTotal) {
            throw IllegalArgumentException("Bad output index $relayIndex")
        }

        Command(moduleIndex, 0x05)
                .append(0.toByte())
                .append(relayIndex.toByte()) // which output
                .append(if (enable) 0x00ff.toShort() else 0x0000.toShort()) // which state
                .send(8)
    }

    private inner class Command(moduleAddress: Byte, functionCode: Byte) {
        private val bytes = ByteArrayOutputStream()
        private val crc = Crc16()

        init {
            write(moduleAddress)
            write(functionCode)
        }

        fun append(byte: Byte): Command {
            write(byte)
            return this
        }

        fun append(short: Short): Command {
            write(short.and(0xff).toInt().toByte())
            write(short.and(0xff00.toShort()).shr(8).toByte())
            return this
        }

        fun send(responseSize: Int): ByteArray {
            append(crc.checksum)
            return connection.send(bytes.toByteArray(), responseSize)
        }

        private fun write(byte: Byte) {
            bytes.write(byte.toInt())
            crc.update(byte)
        }

        fun Short.shr(shift: Int): Int = this.toInt() shr shift
    }
}