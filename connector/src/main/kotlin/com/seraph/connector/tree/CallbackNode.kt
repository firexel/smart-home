package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import script.definition.TreeBuilder

class CallbackNode<T>(
    private val producer: Node.Producer<T>,
    private val callback: TreeBuilder.(T) -> Unit,
    private val builder: TreeBuilder
) : Node {

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            producer.flow.filterNotNull().collect {
                builder.callback(it)
            }
        }
    }
}