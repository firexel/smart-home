package com.seraph.smarthome.device

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import java.util.concurrent.Executor

data class DeviceThreadContext(
        val executor: Executor,
        private val outputPolicy: OutputPolicy = WaitForCrucialInputsPolicy(),
        val id: Device.Id
) {
    inline fun <R> run(block: () -> R): R {
        return executor.run { block() }
    }

    fun inner(newId: String): DeviceThreadContext = copy(id = id.innerId(newId))

    val outputLocked: Boolean
        get() = outputPolicy.locked

    fun setOperationalCallback(operation: () -> Unit) {
        outputPolicy.setOperationalCallback(operation)
    }

    fun waitFotInput(endpoint: Endpoint.Id) {
        outputPolicy.lock(endpoint)
    }

    fun notifyInputGained(endpoint: Endpoint.Id) {
        outputPolicy.unlock(endpoint)
    }
}

interface OutputPolicy {
    val locked: Boolean
    fun lock(endpoint: Endpoint.Id)
    fun unlock(endpoint: Endpoint.Id)
    fun setOperationalCallback(operation: () -> Unit)
}

class WaitForCrucialInputsPolicy : OutputPolicy {

    private var callback: (() -> Unit)? = null
    private val locks = mutableSetOf<Endpoint.Id>()

    override val locked: Boolean
        get() = locks.isNotEmpty()

    override fun lock(endpoint: Endpoint.Id) {
        locks.add(endpoint)
    }

    override fun unlock(endpoint: Endpoint.Id) {
        locks.remove(endpoint)
        val releaseCallback = callback
        if (locks.isEmpty() && releaseCallback != null) {
            releaseCallback()
            callback = null
        }
    }

    override fun setOperationalCallback(operation: () -> Unit) {
        if (locks.isEmpty()) {
            operation()
        } else {
            callback = operation
        }
    }
}