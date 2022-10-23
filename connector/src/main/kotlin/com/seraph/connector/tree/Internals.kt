package com.seraph.connector.tree

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StateFlowProducerNode<T>(
    override val parent: Node,
    initial: T?
) : Node.Producer<T> {
    val stateFlow: MutableStateFlow<T?> = MutableStateFlow(initial)
    override val flow: Flow<T?> = stateFlow

    public var value: T?
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }
}

class StateFlowConsumerNode<T>(
    override val parent: Node,
    initial: T?
) : Node.Consumer<T> {
    val stateFlow: MutableStateFlow<T?> = MutableStateFlow(initial)

    override suspend fun consume(flow: StateFlow<T?>) {
        flow.collect {
            stateFlow.value = it
        }
    }
}

class ConsumerNode<T>(
    override val parent: Node,
    private val consumer: suspend (StateFlow<T?>) -> Unit
) : Node.Consumer<T> {
    override suspend fun consume(flow: StateFlow<T?>) = consumer(flow)
}