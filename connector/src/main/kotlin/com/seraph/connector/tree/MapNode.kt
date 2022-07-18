package com.seraph.connector.tree

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import script.definition.MapContext
import script.definition.Producer

class MapNode<T>(private val block: suspend MapContext.() -> T) : Node {

    private val outputFlow = MutableStateFlow<T?>(null)

    public val output: Node.Producer<T> = object : Node.Producer<T> {
        override val parent: Node
            get() = this@MapNode
        override val flow: Flow<T?>
            get() = outputFlow
    }

    override suspend fun run(scope: CoroutineScope) {
        RunContext(scope, block).schedule()
    }

    inner class RunContext(
        private val scope: CoroutineScope,
        private val block: suspend MapContext.() -> T
    ) : MapContext {

        private val monitors = mutableMapOf<Node.Producer<*>, Deferred<*>>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> monitor(producer: Producer<T>): T {
            val p = producer as Node.Producer<T>
            val deferred = scope.async {
                p.flow.filterNotNull().first()
            }

            installMonitor(p, deferred)

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
                outputFlow.value = supervisorScope { block() }
            }
        }
    }
}