package com.seraph.connector.tree

import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import script.definition.TreeBuilder

class CallbackNode<T>(
    private val callback: TreeBuilder.(T) -> Unit,
    private val builder: TreeBuilder,
    private val log: Log = NoLog()
) : Node {

    val consumer: ConsumerNode<T> = ConsumerNode(this) { flow ->
        flow.filterNotNull()
            .onCompletion { log.w("Completed") }
            .collect {
                builder.callback(it)
            }
    }

    override suspend fun run(scope: CoroutineScope) {
        // not used
    }
}