package com.seraph.connector.tree

import com.seraph.smarthome.util.NoLog
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class TreeHolderTest {

    @Test
    fun testAttachingSingleProducerToASingleConsumer() = runTest { holder ->
        val producer = mockProducer(1)
        val consumer = mockConsumer<Int>()

        holder.connect(producer, consumer)
        consumer.collectedValues.waitFor(listOf(null, 1))

        producer.value = 10
        consumer.collectedValues.waitFor(listOf(null, 1, 10))
    }

    @Test
    fun testAttachingSingleProducerToAMultipleConsumers() = runTest { holder ->
        val producer = mockProducer(1)
        val consumer1 = mockConsumer<Int>()
        val consumer2 = mockConsumer<Int>()

        holder.connect(producer, consumer1)
        holder.connect(producer, consumer2)
        consumer1.collectedValues.waitFor(listOf(null, 1))
        consumer2.collectedValues.waitFor(listOf(null, 1))

        producer.value = 10
        consumer1.collectedValues.waitFor(listOf(null, 1, 10))
        consumer2.collectedValues.waitFor(listOf(null, 1, 10))
    }

    @Test
    fun testStoppedHolderDoNotDeliverValues() = runTest { holder ->
        val producer = mockProducer(1)
        val consumer = mockConsumer<Int>()

        holder.connect(producer, consumer)
        consumer.collectedValues.waitFor(listOf(null, 1))

        producer.value = 10
        consumer.collectedValues.waitFor(listOf(null, 1, 10))

        holder.stop()
        producer.value = 100
        consumer.collectedValues.waitForNoChange(100, listOf(null, 1, 10))
    }

    @Test
    fun testReattachingConsumerShouldDisconnectIfFromFirstProducer() = runTest { holder ->
        val producer1 = mockProducer(1)
        val producer2 = mockProducer(2)
        val consumer = mockConsumer<Int>()

        holder.connect(producer1, consumer)
        consumer.collectedValues.waitFor(listOf(null, 1))

        holder.connect(producer2, consumer)
        consumer.collectedValues.waitFor(listOf(null, 1, 2))

        producer1.value = 10
        consumer.collectedValues.waitForNoChange(100, listOf(null, 1, 2))

        producer2.value = 20
        consumer.collectedValues.waitFor(listOf(null, 1, 2, 20))
    }

    @Test
    fun testNodeCacheShouldReturnSameNodeOnSameKey() = runTest { holder ->
        val node1 = holder.obtain("Key1", Node::class) {
            mockConsumer<Int>().parent
        }
        val node2 = holder.obtain("Key1", Node::class) {
            mockConsumer<Int>().parent
        }
        assert(node1 === node2) { "Nodes are different" }
    }

    @Test
    fun testNodeCacheShouldReturnDifferentNodeOnDifferentKey() = runTest { holder ->
        val node1 = holder.obtain("Key1", Node::class) {
            mockConsumer<Int>().parent
        }
        val node2 = holder.obtain("Key2", Node::class) {
            mockConsumer<Int>().parent
        }
        assert(node1 !== node2) { "Nodes are different" }
    }

    fun runTest(block: suspend (holder: TreeHolder) -> Unit) {
        runBlocking {
            val holder = TreeHolder(NoLog())
            try {
                block(holder)
            } finally {
                holder.stop()
            }
        }
    }
}