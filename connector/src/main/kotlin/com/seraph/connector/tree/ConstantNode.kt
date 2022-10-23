package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ConstantNode<T>(initialValue: T) : Node {

    val value: StateFlowProducerNode<T> = StateFlowProducerNode(this, initialValue)

    override suspend fun run(context: CoroutineScope) {
        // not needed
    }
}