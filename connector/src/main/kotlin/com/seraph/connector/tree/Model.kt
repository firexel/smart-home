package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Node {

    suspend fun run(scope: CoroutineScope)

    interface Child {
        val parent: Node
    }

    interface Producer<T> : Child {
        val flow: Flow<T?>
    }

    interface Consumer<T> : Child {
        suspend fun consume(flow: StateFlow<T?>)
    }
}