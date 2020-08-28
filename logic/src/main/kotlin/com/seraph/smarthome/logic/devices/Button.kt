package com.seraph.smarthome.logic.devices

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.Scheduler
import java.util.concurrent.atomic.AtomicReference

class Button(private val scheduler: Scheduler) : DeviceDriver {

    private val longPressTimeMs = 1000L

    enum class State {
        IDLE, PRESSED
    }

    private var state = AtomicReference(State.IDLE)
    private var delayedTask: Scheduler.Task? = null
    private lateinit var pressAction: DeviceDriver.Output<Unit>
    private lateinit var longPressAction: DeviceDriver.Output<Unit>

    override fun bind(visitor: DeviceDriver.Visitor) {
        pressAction = visitor.declareOutput("press", Types.VOID)
                .setDataKind(Endpoint.DataKind.EVENT)
                .setUserInteraction(Endpoint.Interaction.MAIN)

        longPressAction = visitor.declareOutput("longpress", Types.VOID)
                .setDataKind(Endpoint.DataKind.EVENT)
                .setUserInteraction(Endpoint.Interaction.USER_READONLY)

        visitor.declareInput("key", Types.BOOLEAN).observe {
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
                delayedTask = scheduler.scheduleOnce(longPressTimeMs) { onTimeout() }
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

    private fun changeState(state: State) {
        if (state == State.IDLE) {
            delayedTask?.cancel()
            delayedTask = null
        }
        this.state.set(state)
    }
}