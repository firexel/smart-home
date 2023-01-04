package com.seraph.connector.tree

import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test

internal class MapNodeTest {

    @Test
    fun testBlockResultIsPropagatedToOutput() = runTest {
        val node = MapNode { 42 }
        val flowResults = mutableListOf<Int?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(listOf(null, 42))
    }

    @Test
    fun testBlockGetsProducerValue() = runTest {
        val pA = mockProducer(13)
        val node = MapNode { snapshot(pA) }
        val flowResults = mutableListOf<Int?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(listOf(null, 13))
    }

    @Test
    fun testBlockGetsMultipleMonitorsValue() = runTest {
        val pA = mockProducer(12)
        val pB = mockProducer(34)
        val pC = mockProducer(56)
        val node = MapNode { snapshot(pA) + snapshot(pB) + snapshot(pC) }
        val flowResults = mutableListOf<Int?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(listOf(null, 102))
    }

    @Test
    fun testBlockGetsMultipleMonitorsUpdates() = runTest {
        val pA = mockProducer("A")
        val pB = mockProducer("B")
        val pC = mockProducer("C")
        val node = MapNode { snapshot(pA) + snapshot(pB) + snapshot(pC) }
        val flowResults = mutableListOf<String?>()
        launch { node.output.flow.toCollection(flowResults) }
        launch { node.run(this) }
        flowResults.waitFor(listOf(null, "ABC"))
        pC.value = "c"
        flowResults.waitFor(listOf(null, "ABC", "ABc"))
        pA.value = "a"
        pB.value = "b"
        flowResults.waitFor(listOf(null, "ABC", "ABc", "abc"))
    }
}