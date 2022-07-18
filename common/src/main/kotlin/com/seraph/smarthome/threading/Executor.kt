package com.seraph.smarthome.threading

interface Executor {
    fun run(operation: () -> Unit)
}