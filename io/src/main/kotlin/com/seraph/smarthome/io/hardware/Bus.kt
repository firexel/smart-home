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

        interface Result<out T> {
            val data: T
            val isSuccess: Boolean
            val latency: Long
        }

        class ResultOk<out T>(override val data: T, override val latency: Long) : Result<T> {
            override val isSuccess: Boolean = true
        }

        class ResultError<out T>(private val error: CommunicationException, override val latency: Long) : Result<T> {
            override val data: T
                get() = throw error

            override val isSuccess: Boolean = false
        }
    }

    class CommunicationException(cause: Throwable) : RuntimeException(cause)
}