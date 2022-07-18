package com.seraph.connector.tree

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toCollection
import org.junit.jupiter.api.Test
import script.definition.Producer

internal class MapNodeTest {

    @Test
    fun testBlockResultIsPropagatedToOutput() = runTest {
        val node = MapNode { 42 }
        val flowResults = mutableListOf<Int?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(null, 42)
    }

    @Test
    fun testBlockGetsProducerValue() = runTest {
        val pA = mockProducer(13)
        val node = MapNode { monitor(pA) }
        val flowResults = mutableListOf<Int?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(null, 13)
    }

    @Test
    fun testBlockGetsMultipleMonitorsValue() = runTest {
        val pA = mockProducer(12)
        val pB = mockProducer(34)
        val pC = mockProducer(56)
        val node = MapNode { monitor(pA) + monitor(pB) + monitor(pC) }
        val flowResults = mutableListOf<Int?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(null, 102)
    }

    @Test
    fun testBlockGetsMultipleMonitorsUpdates() = runTest {
        val pA = mockProducer("A")
        val pB = mockProducer("B")
        val pC = mockProducer("C")
        val node = MapNode { monitor(pA) + monitor(pB) + monitor(pC) }
        val flowResults = mutableListOf<String?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(null, "ABC")
        pC.value = "c"
        flowResults.waitFor(null, "ABC", "ABc")
        pA.value = "a"
        pB.value = "b"
        flowResults.waitFor(null, "ABC", "ABc", "abc")
    }

    private suspend fun <T> Collection<T>.waitFor(vararg reference: T) {
        var maxDelays = 300
        val referenceList = mutableListOf(*reference)
        while (this.toMutableList() != referenceList && maxDelays > 0) {
            delay(1)
            maxDelays--
        }
        assert(maxDelays >= 0) { "Waiting for $reference for too long. Actual list is $this" }
    }

    private fun runTest(block: suspend CoroutineScope.() -> Unit) {
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

    class TestFinishesCancellation() : CancellationException()

    fun <T> mockProducer(value: T): MutableProducer<T> {
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

    interface MutableProducer<T> : Producer<T> {
        var value: T
    }
}