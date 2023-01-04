package com.seraph.connector.tree

import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NoLog
import kotlinx.coroutines.*
import script.definition.Timer
import java.lang.Long.max

class TimerNode(
    private val tickInterval: Long,
    private val stopAfter: Long,
    private val log: Log = NoLog()
) : Node, Timer {

    override val active: StateFlowProducerNode<Boolean> = StateFlowProducerNode(this, false)
    override val millisPassed: StateFlowProducerNode<Long> = StateFlowProducerNode(this, 0L)

    private var job: Job? = null
    private var scope: CoroutineScope? = null

    override suspend fun run(scope: CoroutineScope) {
        this.scope = scope

        /**
         * This hack used to keep stored context in Completing state forever to be able to launch
         * coroutines on it in start() method
         */
        scope.launch {
            while (isActive) {
                delay(1000L)
            }
        }
        start()
    }

    override fun start() {
        val prevTimer = job
        job = scope?.launch {
            prevTimer?.let {
                log.v("Cancelling previous")
                it.cancel(RestartCancellation())
                it.join()
            }
            active.value = true
            val startTime = System.currentTimeMillis()
            try {
                while (isActive) {
                    val current = System.currentTimeMillis()
                    if (current < startTime + stopAfter) {
                        millisPassed.value = max(current - startTime, 1)
                        delay(tickInterval)
                    } else {
                        millisPassed.value = stopAfter
                        active.value = false
                        break
                    }
                }
            } catch (ex: StopCancellation) {
                active.value = false
                log.v("Cancelled externally")
            } catch (ex: RestartCancellation) {
                // do nothing
                log.v("Restart exception")
            }
        }
    }

    override fun stop() {
        job?.cancel(StopCancellation())
    }

    class RestartCancellation : CancellationException()
    class StopCancellation : CancellationException()
}