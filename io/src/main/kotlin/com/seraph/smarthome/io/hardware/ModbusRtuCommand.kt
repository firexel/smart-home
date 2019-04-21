package com.seraph.smarthome.io.hardware

import java.io.InputStream
import java.io.OutputStream

abstract class ModbusRtuCommand<out T>(
        private val moduleIndex: Byte,
        private val functionNumber: Byte,
        private val skipTrailingZeros: Boolean = false)
    : Bus.Command<T> {

    override final fun writeRequest(bus: OutputStream) {
        val output = BinaryOutputStream(bus)
        writeRequestHeader(output)
        writeRequestBody(output)
        writeRequestFooter(output)
    }

    private fun writeRequestHeader(output: BinaryOutputStream) {
        output.write(moduleIndex.toInt())
        output.write(functionNumber.toInt())
    }

    abstract fun writeRequestBody(output: BinaryOutputStream)

    private fun writeRequestFooter(output: BinaryOutputStream) {
        output.write(output.checksum)
    }

    override final fun readResponse(bus: InputStream): T {
        val input = BinaryInputStream(bus)
        readResponseHeader(input)
        val responseData = readResponseBody(input)
        readResponseFooter(input)
        return responseData
    }

    private fun readResponseHeader(input: BinaryInputStream) {
        if (skipTrailingZeros && input.readByte().toInt() != 0) {
            throw ModbusRtuFormatException("Zero-byte response start expected")
        }
        if (input.readByte() != moduleIndex) {
            throw ModbusRtuFormatException("Module index expected")
        }
        if (input.readByte() != functionNumber) {
            throw ModbusRtuFormatException("Function number expected")
        }
    }

    abstract fun readResponseBody(input: BinaryInputStream): T

    private fun readResponseFooter(input: BinaryInputStream) {
        input.readShort()
        if (skipTrailingZeros && input.readByte().toInt() != 0) {
            throw ModbusRtuFormatException("Zero-byte response end expected")
        }
    }
}

abstract class ReadInputRegisterCmd<T>(
        addressAtBus: Byte,
        private val registerIndex: Int,
        private val registerCount: Int = 1)
    : ModbusRtuCommand<T>(addressAtBus, 0x04 /* read input */, false) {

    final override fun writeRequestBody(output: BinaryOutputStream) {
        output.write(registerIndex.toShort(), Endianness.MSB_FIRST)
        output.write(registerCount.toShort(), Endianness.MSB_FIRST)
    }

    final override fun readResponseBody(input: BinaryInputStream): T {
        if (input.readByte().toInt() != 2 * registerCount) {
            throw ModbusRtuFormatException("${2 * registerCount} bytes payload expected")
        }
        return dealWithResponse(input)
    }

    abstract fun dealWithResponse(input: BinaryInputStream): T
}

private class ModbusRtuFormatException(msg: String) : RuntimeException(msg)
