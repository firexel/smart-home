package com.seraph.smarthome.device

import com.seraph.smarthome.domain.Control
import com.seraph.smarthome.domain.Endpoint

/**
 * Created by aleksandr.naumov on 28.12.17.
 */
interface DeviceDriver {

    fun configure(visitor: Visitor)

    interface Visitor {
        fun declareInnerDevice(id: String): Visitor
        fun <T> declareInput(id: String, type: Endpoint.Type<T>, retention: Endpoint.Retention): Input<T>
        fun <T> declareOutput(id: String, type: Endpoint.Type<T>, retention: Endpoint.Retention): Output<T>

        fun declareIndicator(id: String, priority: Control.Priority, source: Output<Boolean>)
        fun declareButton(id: String, priority: Control.Priority, input: Input<Unit>, alert: String = "")
    }

    interface Output<in T> {
        fun use(source: () -> T)
        fun invalidate()
    }

    interface Input<out T> {
        fun observe(observer: (T) -> Unit)
    }
}