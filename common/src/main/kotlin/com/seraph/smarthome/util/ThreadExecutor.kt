package com.seraph.smarthome.util

import java.util.concurrent.Executors

class ThreadExecutor: Executor {

    private val threadExecutor = Executors.newSingleThreadExecutor()

    override fun run(action: () -> Unit) {
        threadExecutor.run { action() }
    }
}