package com.seraph.smarthome.threading

import java.util.concurrent.Executors

class ThreadExecutor: Executor {

    private val threadExecutor = Executors.newSingleThreadExecutor()

    override fun run(operation: () -> Unit) {
        threadExecutor.run { operation() }
    }
}