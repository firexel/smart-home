package com.seraph.smarthome.domain

/**
 * Created by aleksandr.naumov on 21.01.18.
 */
data class Button(
        private val trigger: Endpoint<Unit>,
        private val alert: String
) : Control.Usage {

    init {
        if (trigger.direction != Endpoint.Direction.INPUT) {
            throw IllegalArgumentException()
        }
    }

    override fun <T> accept(visitor: Control.Usage.Visitor<T>): T
            = visitor.onButton(trigger, alert)
}

data class Indicator(
        private val source: Endpoint<Boolean>
) : Control.Usage {

    init {
        if (source.direction != Endpoint.Direction.OUTPUT) {
            throw IllegalArgumentException()
        }
    }

    override fun <T> accept(visitor: Control.Usage.Visitor<T>): T
            = visitor.onIndicator(source)
}