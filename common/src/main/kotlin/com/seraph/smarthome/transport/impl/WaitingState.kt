package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import java.lang.Integer.min
import java.util.*

internal class WaitingState(exchanger: Exchanger<SharedData>) : BaseState(exchanger) {

    private var task: TimerTask? = null
    private var expectedReconnectTime: Long = 0

    override fun engage() = sync { data ->
        val task = object : TimerTask() {
            override fun run() {
                transact {
                    it.copy(
                            state = ConnectingState(exchanger),
                            timesRetried = it.timesRetried + 1
                    )
                }
            }
        }
        val delay = calculateDelay(data.timesRetried)
        expectedReconnectTime = System.currentTimeMillis() + delay
        Timer().schedule(task, delay)
        this.task = task
    }

    private fun calculateDelay(timesRetried: Int): Long {
        // 1000 * 2^timesRetried, but not greater than minute
        return 1000.toLong() * min(60, Math.pow(2.toDouble(), timesRetried.toDouble()).toInt())
    }

    override fun disengage() {
        task?.cancel()
        task = null
    }

    override fun <T> accept(visitor: Broker.Visitor<T>): T
            = visitor.onWaitingState(expectedReconnectTime - System.currentTimeMillis())

    override fun execute(action: (Client) -> Unit) = transact {
        it.copy(actions = it.actions + action)
    }
}