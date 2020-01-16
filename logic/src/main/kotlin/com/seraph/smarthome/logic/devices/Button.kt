package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.schedule

class Button(private val timer: Timer) : DeviceDriver {

    private val longPressTimeMs = 1000L

    enum class State {
        IDLE, PRESSED
    }

    private var state = AtomicReference(State.IDLE)
    private var delayedTask: TimerTask? = null
    private lateinit var pressAction: DeviceDriver.Output<Unit>
    private lateinit var longPressAction: DeviceDriver.Output<Unit>

    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareOutputPolicy(DeviceDriver.OutputPolicy.ALWAYS_ALLOW)
        pressAction = visitor.declareOutput("press", Types.VOID, Endpoint.Retention.NOT_RETAINED)
        longPressAction = visitor.declareOutput("longpress", Types.VOID, Endpoint.Retention.NOT_RETAINED)

        visitor.declareInput("key", Types.BOOLEAN, Endpoint.Retention.NOT_RETAINED).observe {
            when (it) {
                true -> onDown()
                false -> onUp()
            }
        }
    }

    private fun onUp() {
        when (state.get()) {
            State.PRESSED -> {
                pressAction.set(Unit)
                changeState(State.IDLE)
            }
            State.IDLE, null -> Unit /* do nothing */
        }
    }

    private fun onDown() {
        when (state.get()) {
            State.IDLE -> {
                changeState(State.PRESSED)
                delayedTask = timer.schedule(longPressTimeMs) { onTimeout() }
            }
            else -> Unit /* do nothing */
        }
    }

    private fun onTimeout() {
        when (state.get()) {
            State.PRESSED -> {
                longPressAction.set(Unit)
                changeState(State.IDLE)
            }
            else -> Unit /* do nothing */
        }
    }

    private fun changeState(state:State) {
        if (state == State.IDLE) {
            delayedTask?.cancel()
            delayedTask = null
        }
        this.state.set(state)
    }
}