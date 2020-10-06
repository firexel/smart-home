package com.seraph.smarthome.util

interface Executor {
    fun run(action: () -> Unit)
}