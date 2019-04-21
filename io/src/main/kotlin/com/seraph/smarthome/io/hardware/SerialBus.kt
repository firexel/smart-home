package com.seraph.smarthome.io.hardware

import com.fazecast.jSerialComm.SerialPort
import com.seraph.smarthome.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeoutException
import kotlin.math.min

/**
 * Created by aleksandr.naumov on 22.04.18.
 */
class SerialBus(
        portName: String,
        settings: Settings,
        private val log: Log
) : Bus {

    private val port: SerialPort

    init {
        port = findPort(portName)
        openPort(settings)
    }

    private fun findPort(portName: String): SerialPort {
        log.i("Scanning for any port of $portName...")
        return SerialPort.getCommPort(portName)
    }

    private fun openPort(settings: Settings) {
        log.i("Found port ${port.systemPortName}. Opening...")
        port.baudRate = settings.baudRate
        port.parity = settings.parity
        port.numDataBits = settings.dataBits
        port.numStopBits = settings.stopBits
        if (!port.openPort()) {
            throw IllegalStateException("Cannot open port ${port.systemPortName}")
        }
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                500, 500
        )
    }

    override fun <T> send(command: Bus.Command<T>): T {
        try {
            writeRequest(command)
            return command.readResponse(SerialPortInputStream(port))
        } catch (t: Throwable) {
            throw t
        }
    }

    private fun <T> writeRequest(command: Bus.Command<T>) {
        val output = ByteArrayOutputStream()
        command.writeRequest(output)
        val bytesToWrite = output.toByteArray()
        port.writeBytes(bytesToWrite, bytesToWrite.size.toLong())
    }

    data class Settings(
            val baudRate: Int = 9600,
            val parity: Int = SerialPort.NO_PARITY,
            val dataBits: Int = 8,
            val stopBits: Int = 2
    )

    inner class SerialPortInputStream(private val port: SerialPort) : InputStream() {
        override fun read(): Int {
            if (waitForBytes() <= 0) {
                throw TimeoutException("Serial port read timeout")
            } else {
                val singleByteArray = ByteArray(1)
                port.readBytes(singleByteArray, 1)
                return singleByteArray[0].toInt()
            }
        }

        override fun read(buffer: ByteArray, readOffset: Int, readLen: Int): Int {
            var bytesToRead = readLen
            while (bytesToRead > 0) {
                val bytesAvailable = waitForBytes()
                if (bytesAvailable == 0) {
                    throw TimeoutException("Serial port read timeout")
                } else {
                    val readBytes = readBytes(min(bytesAvailable, bytesToRead))
                    try {
                        System.arraycopy(readBytes, 0, buffer, readOffset + readLen - bytesToRead, readBytes.size)
                    } catch (t: Throwable) {
                        log.v("Buffer size: ${buffer.size} readOffset: $readOffset read len: $readLen\n" +
                                "System.arraycopy($readBytes, 0, $buffer, ${readOffset + readLen - bytesToRead}, ${readBytes.size}")
                        throw t
                    }
                    bytesToRead -= readBytes.size
                }
            }
            return readLen
        }

        private fun readBytes(bytesToRead: Int): ByteArray {
            val byteArray = ByteArray(bytesToRead)
            port.readBytes(byteArray, bytesToRead.toLong())
            return byteArray
        }

        private fun waitForBytes(): Int {
            val timeBeforeWait = System.currentTimeMillis()
            while (port.bytesAvailable() <= 0
                    && (timeBeforeWait + port.readTimeout) > System.currentTimeMillis()) {
                Thread.sleep(5)
            }
            return port.bytesAvailable()
        }
    }
}