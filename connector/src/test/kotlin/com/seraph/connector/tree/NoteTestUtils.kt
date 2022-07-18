package com.seraph.connector.tree

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.fail
import script.definition.Producer


internal suspend fun <T> List<T>.waitFor(reference: List<T>) =
    waitFor(300, reference)

internal suspend fun <T> List<T>.waitFor(timeout: Long, reference: List<T>) =
    waitFor(timeout, { a, b -> a == b }, reference)

internal suspend fun <T> List<T>.waitFor(
    timeout: Long,
    comparator: (T, T) -> Boolean,
    reference: List<T>
) {
    val endTime = System.currentTimeMillis() + timeout
    while (true) {
        val currentTime = System.currentTimeMillis()
        if (this.compare(reference, comparator)) {
            return
        } else if (currentTime > endTime) {
            fail { "Waiting for $reference for too long. Actual list is $this" }
        }
        delay(10)
    }
}

internal fun <T> List<T>.compare(second: List<T>, comparator: (T, T) -> Boolean): Boolean {
    if (this.size != second.size) {
        return false
    }
    this.zip(second).forEachIndexed { i, p ->
        if (!comparator(p.first, p.second)) {
            return false
        }
    }
    return true
}

internal fun runTest(block: suspend CoroutineScope.() -> Unit) {
    return runBlocking {
        try {
            supervisorScope {
                block()
                cancel(TestFinishesCancellation())
            }
        } catch (ex: TestFinishesCancellation) {
            // test pass
        }
    }
}

internal class TestFinishesCancellation() : CancellationException()

internal fun <T> mockProducer(value: T): MutableProducer<T> {
    val parent = object : Node {
        override suspend fun run(scope: CoroutineScope) {
            // noop
        }
    }
    return object : Producer<T>, Node.Producer<T>, MutableProducer<T> {
        private val _flow = MutableStateFlow(value)
        override val parent: Node
            get() = parent
        override val flow: Flow<T?>
            get() = _flow
        override var value: T
            get() = _flow.value
            set(value) {
                _flow.value = value
            }
    }
}

internal interface MutableProducer<T> : Producer<T> {
    var value: T
}