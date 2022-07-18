package com.seraph.connector.tree

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import script.definition.Producer
import script.definition.Timer
import java.lang.Long.max

class TimerNode(
    private val tickInterval: Long,
    private val stopAfter: Long
) : Node, Timer {

    private val activeState = MutableStateFlow(false)
    private val millisState = MutableStateFlow(0L)
    private var job: Job? = null
    private var scope: CoroutineScope? = null

    override suspend fun run(scope: CoroutineScope) {
        this.scope = scope
        start()
    }

    override fun start() {
        val prevTimer = job
        job = scope?.launch {
            prevTimer?.let {
                it.cancel(RestartCancellation())
                it.join()
            }
            activeState.value = true
            val startTime = System.currentTimeMillis()
            try {
                while (isActive) {
                    val current = System.currentTimeMillis()
                    if (current < startTime + stopAfter) {
                        millisState.value = max(current - startTime, 1)
                        delay(tickInterval)
                    } else {
                        millisState.value = stopAfter
                        activeState.value = false
                        break
                    }
                }
            } catch (ex: StopCancellation) {
                activeState.value = false
            } catch (ex: RestartCancellation) {
                // do nothing
            }
        }
    }

    override fun stop() {
        job?.cancel(StopCancellation())
    }

    override val active: Producer<Boolean> = object : Producer<Boolean>, Node.Producer<Boolean> {
        override val parent: Node
            get() = this@TimerNode
        override val flow: Flow<Boolean?>
            get() = activeState
    }

    override val millisPassed: Producer<Long> = object : Producer<Long>, Node.Producer<Long> {
        override val parent: Node
            get() = this@TimerNode
        override val flow: Flow<Long?>
            get() = millisState
    }

    class RestartCancellation : CancellationException()
    class StopCancellation : CancellationException()
}