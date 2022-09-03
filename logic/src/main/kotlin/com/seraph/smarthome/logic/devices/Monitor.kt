package com.seraph.smarthome.logic.devices

import com.google.gson.annotations.SerializedName
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.lang.Long.max

sealed class State {
    class Timeout : State()
    class Value(val value: Float) : State()
}

class Monitor(private val settings: Settings) : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        val current = visitor.declareInput("current_in", Types.FLOAT)
            .waitForDataBeforeOutput()
            .observeAsChannel(BufferOverflow.DROP_OLDEST)

        val trigger = visitor.declareOutput("trigger_out", Types.BOOLEAN)
        val alert = visitor.declareOutput("alert_text_out", Types.STRING)
            .setDataKind(Endpoint.DataKind.EVENT)

        visitor.onOperational {
            GlobalScope.launch {
                monitorInfinitely(current, trigger, alert)
            }
        }
    }

    private suspend fun monitorInfinitely(
        current: Channel<Float>,
        trigger: DeviceDriver.Output<Boolean>,
        alert: DeviceDriver.Output<String>
    ) {
        while (true) {
            when (val state = monitor(settings.receiveTimeout, current)) {
                is State.Timeout -> {
                    trigger.set(true)
                    alert.set(noReceiveMessage())
                }
                is State.Value -> {
                    if (shouldTrigger(state.value) && !waitForGoodValue(current)) {
                        trigger.set(true)
                        alert.set(clauseTriggerMessage(state.value))
                    } else {
                        trigger.set(false)
                    }
                }
            }
        }
    }

    private suspend fun waitForGoodValue(current: Channel<Float>): Boolean {
        val waitStart = now()
        val waitEnd = waitStart + settings.timeToTrigger
        while (waitEnd > now()) {
            when (val state = monitor(max(waitEnd - now(), 1L), current)) {
                is State.Timeout -> {
                    return false
                }
                is State.Value -> {
                    if (!shouldTrigger(state.value)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun now() = System.currentTimeMillis()

    private fun shouldTrigger(value: Float) = when (settings.clause) {
        Clause.GREATER -> value > settings.target
        Clause.LESS -> value < settings.target
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun monitor(
        timeout: Long,
        current: Channel<Float>
    ): State {
        return select {
            if (timeout > 0) {
                onTimeout(timeout) { State.Timeout() }
            }
            current.onReceiveCatching {
                State.Value(it.getOrThrow())
            }
        }
    }

    private fun noReceiveMessage(): String = when (settings.noReceiveAlert) {
        "" -> "No values update received in ${settings.receiveTimeout}ms"
        else -> settings.noReceiveAlert
    }

    private fun clauseTriggerMessage(value: Float): String = when (settings.clauseTriggerAlert) {
        "" -> {
            val clauseStr = when (settings.clause) {
                Clause.GREATER -> "greater"
                Clause.LESS -> "less"
            }
            "Value was $clauseStr than ${settings.target} for a ${settings.timeToTrigger}ms. " +
                    "First was $value"
        }
        else -> settings.clauseTriggerAlert
    }

    data class Settings(
        val clause: Clause,
        val target: Float,

        @SerializedName("time_to_trigger_millis")
        val timeToTrigger: Long,

        @SerializedName("receive_timeout_millis")
        val receiveTimeout: Long = 10000,

        @SerializedName("no_receive_alert")
        val noReceiveAlert: String = "",

        @SerializedName("clause_trigger_alert")
        val clauseTriggerAlert: String = ""
    )

    enum class Clause {
        GREATER, LESS
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> DeviceDriver.Input<T>.observeAsFlow(): Flow<T> {
    val input = this
    return callbackFlow {
        input.observe {
            trySendBlocking(it)
        }
        awaitClose { /* nothing to do */ }
    }
}

fun <T> DeviceDriver.Input<T>.observeAsChannel(overflow: BufferOverflow = BufferOverflow.SUSPEND): Channel<T> {
    val channel = Channel<T>(onBufferOverflow = overflow)
    this.observe {
        channel.trySendBlocking(it)
    }
    return channel
}