package com.seraph.connector.tree

import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import script.definition.TreeBuilder

class CallbackNode<T>(
    private val callback: TreeBuilder.(T) -> Unit,
    private val builder: TreeBuilder,
    private val log: Log = NoLog()
) : Node {

    val consumer: Node.Consumer<T> = object : Node.Consumer<T> {
        override val parent: Node
            get() = this@CallbackNode

        override suspend fun consume(flow: StateFlow<T?>) {
            flow.filterNotNull()
                .onCompletion { log.w("Completed") }
                .collect {
                    builder.callback(it)
                }
        }
    }

    override suspend fun run(scope: CoroutineScope) {
        // not used
    }
}