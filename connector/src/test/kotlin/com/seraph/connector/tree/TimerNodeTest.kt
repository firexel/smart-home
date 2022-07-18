package com.seraph.connector.tree

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import kotlin.math.abs

@Suppress("UNCHECKED_CAST")
internal class TimerNodeTest {

    @Test
    fun testStatesAutoStartAutoStop() = runTest {
        val node = TimerNode(100, 500)
        node.run(this)
        val states = mutableListOf<Boolean?>()
        launch { (node.active as Node.Producer<Boolean>).flow.toCollection(states) }
        states.waitFor(listOf(true))
        delay(300)
        states.waitFor(listOf(true, false))
    }

    @Test
    fun testStatesAutoStartManualStop() = runTest {
        val node = TimerNode(100, 500)
        val states = mutableListOf<Boolean?>()
        launch { (node.active as Node.Producer<Boolean>).flow.toCollection(states) }

        node.run(this)
        delay(150)

        node.stop()
        states.waitFor(10L, listOf(false, true, false))
    }

    @Test
    fun testStatesRestart() = runTest {
        val node = TimerNode(100, 500)
        val states = mutableListOf<Boolean?>()
        launch { (node.active as Node.Producer<Boolean>).flow.toCollection(states) }

        node.run(this)
        delay(150)
        states.waitFor(10L, listOf(false, true))

        node.start()
        delay(400)
        states.waitFor(10L, listOf(false, true))
        states.waitFor(600L, listOf(false, true, false))
    }

    @Test
    fun testMillisAutoStartAutoStop() = runTest {
        val node = TimerNode(100, 500)
        node.run(this)
        val millis = mutableListOf<Long?>()
        launch { (node.millisPassed as Node.Producer<Long>).flow.toCollection(millis) }
        millis.waitFor(600L, INACCURATE, listOf(1, 100, 200, 300, 400, 500))
    }

    @Test
    fun testMillisAutoStartRestart() = runTest {
        val node = TimerNode(100, 500)
        val millis = mutableListOf<Long?>()
        launch { (node.millisPassed as Node.Producer<Long>).flow.toCollection(millis) }

        node.run(this)
        delay(250)
        node.start()

        millis.waitFor(600L, INACCURATE, listOf(0, 1, 100, 200, 1, 100, 200, 300, 400, 500))
    }

    @Test
    fun testMillisAutoStartStop() = runTest {
        val node = TimerNode(100, 500)
        val millis = mutableListOf<Long?>()
        launch { (node.millisPassed as Node.Producer<Long>).flow.toCollection(millis) }

        node.run(this)
        delay(250)
        node.stop()

        millis.waitFor(600L, INACCURATE, listOf(0, 1, 100, 200))
    }

    val INACCURATE: (Long?, Long?) -> Boolean =
        { a, b -> abs(a!! - b!!) < 15 }
}