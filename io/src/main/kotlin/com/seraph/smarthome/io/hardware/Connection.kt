package com.seraph.smarthome.io.hardware

/**
 * Created by aleksandr.naumov on 24.12.17.
 */
interface Connection {
    fun send(byteArray: ByteArray, responseSize: Int): ByteArray
}