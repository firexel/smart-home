package com.seraph.connector.tree

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import script.definition.MapContext

class MapNode<T>(private val block: suspend MapContext.() -> T) : Node {

    val output: StateFlowProducerNode<T> = StateFlowProducerNode(this, null)

    override suspend fun run(scope: CoroutineScope) {
        RunContext(scope, block).schedule()
    }

    inner class RunContext(
        private val scope: CoroutineScope,
        private val block: suspend MapContext.() -> T
    ) : MapContext {

        private val monitors = mutableMapOf<Node.Producer<*>, Deferred<*>>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> snapshot(producer: Node.Producer<T>): T {
            val deferred = scope.async {
                producer.flow.filterNotNull().first()
            }

            installMonitor(producer, deferred)

            return deferred.await()
        }

        private fun installMonitor(
            producer: Node.Producer<*>,
            deferred: Deferred<*>
        ) {
            if (!monitors.contains(producer)) {
                monitors[producer] = deferred
                producer.flow.onEach {
                    monitors.entries.forEach {
                        if (it.key != producer && it.value.isActive) {
                            it.value.cancel("Cancelled due to updates in $producer")
                        }
                    }
                    schedule()
                }.launchIn(scope)
            } else {
                monitors[producer] = deferred
            }
        }

        fun schedule() {
            scope.launch {
                output.value = supervisorScope { block() }
            }
        }
    }
}