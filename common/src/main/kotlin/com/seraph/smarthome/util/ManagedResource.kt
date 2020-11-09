package com.seraph.smarthome.util

import java.lang.Thread.sleep
import java.util.concurrent.Executors

class ManagedResource<T>(private val operator: ResourceOperator<T>, log: Log) {

    private val executor = Executors.newSingleThreadExecutor()
    private var resource: T? = null

    init {
        executor.submit {
            this@ManagedResource.resource = createResourceSync()
        }
    }

    private fun createResourceSync(): T {
        while (true) {
            try {
                return operator.setUp()
            } catch (th: Throwable) {
                sleep(1000)
            }
        }
    }

    fun operate(action: (T) -> Unit) {
//        executor.submit {
//            try {
//                var resource = resource
//                if (resource == null) {
//                    resource = createResourceSync()
//                    this@ManagedResource.resource = resource
//                }
//                try {
//                    action(resource!!)
//                } catch (th: Throwable) {
////                    when (operator.classifyFailure(th)) {
////
////                    }
//                }
//            }
//        }
    }

    interface ResourceOperator<T> {
        fun setUp(): T
        fun tearDown(resource: T)
        fun classifyFailure(ex: Throwable): FailureAction
    }

    enum class FailureAction {
        RECREATE, NOOP, RETRY
    }
}