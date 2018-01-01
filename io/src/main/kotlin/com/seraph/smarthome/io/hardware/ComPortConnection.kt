package com.seraph.smarthome.io.hardware

import com.fazecast.jSerialComm.SerialPort
import com.seraph.smarthome.util.Log

class ComPortConnection(
        portName: String,
        settings: Settings,
        private val log: Log
) : Connection {

    private val port: SerialPort

    init {
        log.i("Scanning for any port of $portName...")

        port = SerialPort.getCommPorts().find { it.systemPortName.contains(portName) }
                ?: throw IllegalArgumentException("Cannot find port $portName")

        log.i("Found port ${port.systemPortName}. Opening...")

        port.baudRate = settings.baudRate
        port.parity = settings.parity
        port.numDataBits = settings.dataBits
        port.numStopBits = settings.stopBits
        if (!port.openPort()) {
            throw IllegalStateException("Cannot open port ${port.systemPortName}")
        }
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING and SerialPort.TIMEOUT_WRITE_SEMI_BLOCKING,
                100, 100
        )
    }

    override fun send(byteArray: ByteArray, responseSize: Int): ByteArray = synchronized(this) {
        sendRequest(byteArray)
        receiveResponse(responseSize, byteArray)
    }

    private fun sendRequest(byteArray: ByteArray) {
        log.i("com < ${byteArray.asHexString()}")
        port.writeBytes(byteArray, byteArray.size.toLong())
    }

    private fun receiveResponse(responseSize: Int, byteArray: ByteArray): ByteArray {
        val response = ByteArray(responseSize)
        val timeBeforeWait = System.currentTimeMillis()
        while (port.bytesAvailable() < responseSize
                && (timeBeforeWait + port.readTimeout) > System.currentTimeMillis()) {
            Thread.sleep(1)
        }
        val bytesReceived = port.readBytes(response, responseSize.toLong())
        when {
            bytesReceived < 0 -> log.w("com port read error")
            bytesReceived < responseSize -> log.w("com port read timeout ${port.readTimeout}")
            else -> log.i("com > ${response.asHexString()}")
        }
        return response
    }

    data class Settings(
            val baudRate: Int = 9200,
            val parity: Int = SerialPort.EVEN_PARITY,
            val dataBits: Int = 8,
            val stopBits: Int = 1
    )
}