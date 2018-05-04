package com.seraph.smarthome.io.hardware

import java.io.InputStream
import java.io.OutputStream

/**
 * Created by aleksandr.naumov on 22.04.18.
 */

interface Bus {
    fun <T> send(command: Command<T>): T

    interface Command<out T> {
        fun writeRequest(bus: OutputStream)
        fun readResponse(bus: InputStream): T
    }
}