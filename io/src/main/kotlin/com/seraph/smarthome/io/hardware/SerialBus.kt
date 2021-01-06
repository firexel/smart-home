package com.seraph.smarthome.io.hardware

import com.fazecast.jSerialComm.SerialPort
import com.seraph.smarthome.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 22.04.18.
 */
class SerialBus(
        private val portName: String,
        private val settings: Settings,
        private val log: Log,
) : Bus {

    private var port: SerialPort
    private var portCleans: Int = 0

    init {
        port = openPort()
    }

    private val singleIoTimeoutMs = 500
    private val cleanIoTimeoutMs = 2000

    private fun openPort(): SerialPort {
        log.i("Scanning for any port of $portName...")
        val port = SerialPort.getCommPort(portName)

        log.i("Found port ${port.systemPortName}. Opening...")
        port.baudRate = settings.baudRate
        port.parity = settings.parity
        port.numDataBits = settings.dataBits
        port.numStopBits = settings.stopBits
        port.setRs485ModeParameters(true, true, 200, 200)
        if (!port.openPort()) {
            throw IllegalStateException("Cannot open port ${port.systemPortName}")
        }
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                singleIoTimeoutMs, singleIoTimeoutMs
        )
        return port
    }

    override fun <T> send(command: Bus.Command<T>): T {
        try {
            writeRequest(command)
            val inputStream = SerialPortInputStream(port)
            return command.readResponse(inputStream)
        } catch (err: Throwable) {
            if (!port.isOpen || err.causeIs(PortNotOpenException::class)) {
                reopenPort()
            } else if (err.causeIs(PortIoException::class)) {
                if (portCleans < 3) {
                    portCleans++
                    cleanPort()
                } else {
                    portCleans = 0
                    err.printStackTrace()
                    reopenPort()
                }
            }
            throw err
        }
    }

    private fun reopenPort() {
        log.w("Reopening a port...")
        port.closePort()
        port = openPort()
    }

    private fun cleanPort() {
        log.w("Cleaning a port...")
        val stream = SerialPortInputStream(port)
        waitUnderTimeout(cleanIoTimeoutMs) {
            try {
                stream.read()
            } catch (t: Throwable) {
                //ignore
            }
            true
        }
    }

    private fun <T> writeRequest(command: Bus.Command<T>) {
        val output = ByteArrayOutputStream()
        command.writeRequest(output)
        val bytesToWrite = output.toByteArray()
        val writeResult = port.writeBytes(bytesToWrite, bytesToWrite.size.toLong())
        when {
            writeResult < 0 -> throw PortIoException("Error writing to port: $writeResult")
            writeResult < bytesToWrite.size -> throw PortIoException("Cannot write to port ${bytesToWrite.size} bytes")
        }
        waitUnderTimeout(1000) {
            port.bytesAwaitingWrite() <= 0
        }
        if (port.bytesAwaitingWrite() > 0) {
            throw PortIoException("Error writing to port")
        }
    }

    data class Settings(
            val baudRate: Int = 9600,
            val parity: Int = SerialPort.NO_PARITY,
            val dataBits: Int = 8,
            val stopBits: Int = 1,
    )

    inner class SerialPortInputStream(private val port: SerialPort) : InputStream() {

        override fun read(): Int {
            waitForBytes()
            val singleByteArray = ByteArray(1)
            port.readBytes(singleByteArray, 1)
            return singleByteArray[0].toInt()
        }

        override fun read(buffer: ByteArray, readOffset: Int, readLen: Int): Int {
            var bytesToRead = readLen
            while (bytesToRead > 0) {
                val bytesAvailable = waitForBytes()
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
            return readLen
        }

        private fun readBytes(bytesToRead: Int): ByteArray {
            val byteArray = ByteArray(bytesToRead)
            port.readBytes(byteArray, bytesToRead.toLong())
            return byteArray
        }

        private fun waitForBytes(): Int {
            waitUnderTimeout(port.readTimeout) {
                port.bytesAvailable() > 0
            }
            val bytes = port.bytesAvailable()
            when {
                bytes < 0 -> throw PortNotOpenException()
                bytes == 0 -> throw PortIoException("Serial port read timeout")
                else -> return bytes
            }
        }
    }

    private open class PortIoException(msg: String? = null) : IOException(msg)

    private class PortNotOpenException : PortIoException()

    private fun waitUnderTimeout(msToWait: Int, predicate: () -> Boolean) {
        val timeBeforeWait = System.currentTimeMillis()
        while (!predicate() && (timeBeforeWait + msToWait) > System.currentTimeMillis()) {
            Thread.sleep(5)
        }
    }

    private fun <T : Throwable> Throwable.causeIs(c: KClass<T>): Boolean {
        val cause = cause
        return c.isInstance(this) || c.isInstance(cause) || cause?.causeIs(c) ?: false
    }
}