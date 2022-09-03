package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import script.definition.Producer
import kotlin.coroutines.CoroutineContext

class ConstantNode<T>(initialValue: T) : Node {

    val value: Node.Producer<T> = object : Node.Producer<T>, Producer<T> {
        override val parent: Node
            get() = this@ConstantNode

        override val flow: Flow<T?>
            get() = MutableStateFlow(initialValue)
    }

    override suspend fun run(context: CoroutineScope) {
        // not needed
    }
}