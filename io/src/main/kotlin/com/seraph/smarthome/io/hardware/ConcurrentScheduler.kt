package com.seraph.smarthome.io.hardware

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
class ConcurrentScheduler(private val bus: Bus) : Scheduler {

    private val executor = Executors.newScheduledThreadPool(1)

    override fun <T> post(cmd: Bus.Command<T>, delay: Long, callback: (Bus.Command.Result<T>) -> Unit) {
        executor.schedule(CommandCallable(cmd, callback), max(delay, 1), TimeUnit.MILLISECONDS)
    }

    inner class CommandCallable<T>(
            private val cmd: Bus.Command<T>,
            private val callback: (Bus.Command.Result<T>) -> Unit)
        : Runnable {

        override fun run() {
            val reqStart = System.nanoTime()
            try {
                callback(Bus.Command.ResultOk(bus.send(cmd), (System.nanoTime() - reqStart) / 1000000))
            } catch (ex: Exception) {
                callback(Bus.Command.ResultError(Bus.CommunicationException(ex), (System.nanoTime() - reqStart) / 1000000))
            }
        }
    }
}
