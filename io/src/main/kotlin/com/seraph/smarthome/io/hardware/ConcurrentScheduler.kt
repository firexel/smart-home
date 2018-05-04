package com.seraph.smarthome.io.hardware

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
class ConcurrentScheduler(private val bus: Bus) : Scheduler {

    private val executor = Executors.newScheduledThreadPool(1)

    override fun <T> post(cmd: Bus.Command<T>, delay: Long, callback: (T) -> Unit) {
        executor.schedule(CommandCallable(cmd, callback), delay, TimeUnit.MILLISECONDS)
    }

    inner class CommandCallable<T>(
            private val cmd: Bus.Command<T>,
            private val callback: (T) -> Unit)
        : Runnable {

        override fun run() {
            callback(bus.send(cmd))
        }
    }
}
