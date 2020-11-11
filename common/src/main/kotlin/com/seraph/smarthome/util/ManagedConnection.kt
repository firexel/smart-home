package com.seraph.smarthome.util

import java.io.PrintWriter
import java.net.Socket

class ManagedConnection(host: String, port: Int, log: Log) {

    private val connection = ManagedResource(SocketOperator(host, port), log)

    fun send(string: String) {
        connection.operate {
            it.out.print(string)
        }
    }

    class SocketOperator(
            private val host: String,
            private val port: Int
    ) : ManagedResource.ResourceOperator<SocketOperator.ConnectionState> {

        data class ConnectionState(
                val socket: Socket,
                val out: PrintWriter
        )

        override fun setUp(): ConnectionState {
            val socket = Socket(host, port)
            val out = PrintWriter(socket.getOutputStream(), true)
            return ConnectionState(socket, out)
        }

        override fun tearDown(resource: ConnectionState) {
            resource.out.close()
            resource.socket.close()
        }

        override fun mitigateFailure(resource: ConnectionState?, ex: Throwable, failuresSinceOperational: Int)
                : Iterable<ManagedResource.FailureMitigation> {

            return if (failuresSinceOperational < 3 && resource != null && !resource.socket.isClosed) {
                val millisToWait = mapTimesToMillis(failuresSinceOperational)
                listOf(ManagedResource.FailureMitigation.Sleep(millisToWait))
            } else {
                listOf(ManagedResource.FailureMitigation.Recreate())
            }
        }

        private fun mapTimesToMillis(failuresSinceOperational: Int): Long {
            return when (failuresSinceOperational) {
                0 -> 0L
                1 -> 1000L
                2 -> 3000L
                else -> 5000L
            }
        }
    }
}