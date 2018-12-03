package com.seraph.smarthome.io.hardware

/**
 * Created by aleksandr.naumov on 02.05.18.
 */
interface Scheduler {
    fun <T> post(
            cmd: Bus.Command<T>,
            delay: Long = 0,
            callback: (Bus.Command.Result<T>) -> Unit = {}
    )
}