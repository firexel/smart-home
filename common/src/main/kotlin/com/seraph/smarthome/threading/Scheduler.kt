package com.seraph.smarthome.threading

interface Scheduler : Executor {

    fun schedulePeriodically(cycleLengthMs: Long, operation: () -> Unit) : Task

    fun scheduleOnce(timeToWaitMs: Long, operation: () -> Unit) : Task

    interface Task {
        fun cancel()
    }
}