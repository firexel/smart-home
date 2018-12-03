package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.transport.Broker
import java.lang.Integer.min
import java.util.*

internal class WaitingState(
        exchanger: Exchanger<BaseState, SharedData>,
        private val clock: Clock = SystemClock()
) : BaseState(exchanger) {

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
        expectedReconnectTime = clock.time + delay
        Timer().schedule(task, delay)
        this.task = task
    }

    private fun calculateDelay(timesRetried: Int): Long {
        return 2.pow(timesRetried, 60) * 1000.toLong()
    }

    private fun Int.pow(power: Int, maximum: Int): Int {
        return when {
            power < 0 -> throw IllegalArgumentException()
            power == 0 -> min(maximum, 1)
            else -> {
                var result = this
                for (i in 1 until power) {
                    if (result >= maximum) break
                    result *= this
                }
                min(maximum, result)
            }
        }
    }

    override fun disengage() {
        task?.cancel()
        task = null
    }

    override fun <T> accept(visitor: Broker.BrokerState.Visitor<T>): T
            = visitor.onWaitingState(expectedReconnectTime - clock.time)

    override fun execute(key: Any?, action: (Client) -> Unit) = transact {
        it.copy(actions = it.actions + SharedData.Action(key, action))
    }

    interface Clock {
        val time: Long
    }

    class SystemClock : Clock {
        override val time: Long
            get() = System.currentTimeMillis()
    }
}