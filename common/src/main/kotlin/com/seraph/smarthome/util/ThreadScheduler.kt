package com.seraph.smarthome.util

import java.util.*

class ThreadScheduler(name: String) : Scheduler {

    private val timer = Timer(name)

    override fun schedulePeriodically(cycleLengthMs: Long, operation: () -> Unit): Scheduler.Task {
        val task = wrap(operation)
        timer.scheduleAtFixedRate(task, 0L, cycleLengthMs)
        return Task(task)
    }

    override fun scheduleOnce(timeToWaitMs: Long, operation: () -> Unit): Scheduler.Task {
        val task = wrap(operation)
        timer.schedule(task, timeToWaitMs)
        return Task(task)
    }

    private fun wrap(operation: () -> Unit): TimerTask {
        return object : TimerTask() {
            override fun run() {
                operation()
            }
        }
    }

    private class Task(private val wrapped: TimerTask) : Scheduler.Task {
        override fun cancel() {
            wrapped.cancel()
        }
    }
}