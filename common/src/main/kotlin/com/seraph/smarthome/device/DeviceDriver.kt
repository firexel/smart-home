package com.seraph.smarthome.device

import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Units

/**
 * Created by aleksandr.naumov on 28.12.17.
 */
interface DeviceDriver {

    fun bind(visitor: Visitor)

    interface Visitor {
        fun declareInnerDevice(id: String): Visitor

        fun <T> declareInput(id: String, type: Endpoint.Type<T>): Input<T>
        fun <T> declareOutput(id: String, type: Endpoint.Type<T>): Output<T>

        fun onOperational(operation: () -> Unit)
    }

    interface Output<in T> {
        fun set(update: T)

        fun setDataKind(dataKind: Endpoint.DataKind): Output<T>
        fun setUserInteraction(interaction: Endpoint.Interaction): Output<T>
        fun setUnits(units: Units): Output<T>
    }

    interface Input<T> {
        fun observe(observer: (T) -> Unit)

        fun setDataKind(dataKind: Endpoint.DataKind): Input<T>
        fun setUserInteraction(interaction: Endpoint.Interaction): Input<T>
        fun setUnits(units: Units): Input<T>

        fun waitForDataBeforeOutput(): Input<T>
    }
}