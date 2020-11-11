package com.seraph.smarthome.util

import java.lang.Thread.sleep
import java.util.concurrent.Executors

class ManagedResource<T>(
        private val operator: ResourceOperator<T>,
        private val log: Log
) {

    private val executor = Executors.newSingleThreadExecutor()
    private var resource: T? = null
    private var failuresSinceOperational = 0

    init {
        operate { log.i("$it created successfully") }
    }

    private fun createResourceSync(): T {
        while (true) {
            try {
                return operator.setUp()
            } catch (th: Throwable) {
                reportFailureAndGetMitigations(th).forEach { action ->
                    when (action) {
                        is FailureMitigation.Sleep -> sleepSync(action.millis)
                    }
                }
            }
        }
    }

    private fun destroyResourceSync() {
        val resource = resource
        if (resource != null) {
            operator.tearDown(resource)
            this.resource = null
        } else {
            log.w("Resource was deleted somehow")
        }
    }

    private fun sleepSync(millis: Long) {
        sleep(millis)
    }

    fun operate(action: (T) -> Unit) {
        executor.submit {
            var resource = resource
            if (resource == null) {
                resource = createResourceSync()
                this@ManagedResource.resource = resource
            }
            try {
                action(resource!!)
                failuresSinceOperational = 0
            } catch (th: Throwable) {
                reportFailureAndGetMitigations(th).forEach { action ->
                    when (action) {
                        is FailureMitigation.Recreate -> destroyResourceSync()
                        is FailureMitigation.Sleep -> sleepSync(action.millis)
                    }
                }
            }
        }
    }

    private fun reportFailureAndGetMitigations(th: Throwable): List<FailureMitigation> {
        failuresSinceOperational++
        return operator.mitigateFailure(resource, th, failuresSinceOperational)
                .toSet()
                .toList()
                .sortedBy { it.priority }
    }

    interface ResourceOperator<T> {
        fun setUp(): T
        fun tearDown(resource: T)
        fun mitigateFailure(resource: T?, ex: Throwable, failuresSinceOperational: Int): Iterable<FailureMitigation>
    }

    sealed class FailureMitigation(val priority: Int) {

        data class Sleep(val millis: Long) : FailureMitigation(0)

        class Recreate : FailureMitigation(1) {
            override fun equals(other: Any?): Boolean {
                return this === other
            }

            override fun hashCode(): Int {
                return System.identityHashCode(this)
            }
        }
    }
}