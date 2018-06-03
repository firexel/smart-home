package com.seraph.smarthome.io.hardware

import java.io.InputStream
import java.io.OutputStream

abstract class ModbusRtuCommand<out T>(
        private val moduleIndex: Byte,
        private val functionNumber: Byte)
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
        input.readByte() // for module index
        input.readByte() // for function number
    }

    abstract fun readResponseBody(input: BinaryInputStream): T

    private fun readResponseFooter(input: BinaryInputStream) {
        input.readShort()
    }
}