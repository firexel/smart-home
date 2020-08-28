package com.seraph.smarthome.util

interface Scheduler {

    fun schedulePeriodically(cycleLengthMs: Long, operation: () -> Unit) : Task

    fun scheduleOnce(timeToWaitMs: Long, operation: () -> Unit) : Task

    interface Task {
        fun cancel()
    }
}