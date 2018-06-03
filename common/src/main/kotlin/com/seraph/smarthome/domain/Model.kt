package com.seraph.smarthome.domain


/**
 * Created by aleksandr.naumov on 29.12.17.
 */

data class Metainfo(
        val brokerName: String
)

data class Device(
        val id: Id,
        val endpoints: List<Endpoint<*>> = emptyList(),
        val controls: List<Control> = emptyList()
) {
    data class Id(val segments: List<String>) {
        constructor(vararg segments: String) : this(segments.toList())

        val value: String = segments.joinToString(":")
        override fun toString(): String = value
        fun innerId(newSegment: String) = Id(segments + newSegment)
    }
}

data class Control(
        val id: Id,
        val priority: Priority,
        val usage: Usage
) {
    data class Id(val value: String)

    enum class Priority { MAIN, PRIMARY, SECONDARY }

    interface Usage {
        fun <T> accept(visitor: Visitor<T>): T

        interface Visitor<out T> {
            fun onButton(trigger: Endpoint<Unit>, alert: String = ""): T
            fun onIndicator(source: Endpoint<Boolean>): T
        }
    }
}

data class Endpoint<N>(
        val id: Id,
        val type: Type<N>,
        val direction: Direction,
        val retention: Retention,
        val units: Units = Units.NO
) {
    data class Id(val value: String)

    enum class Retention { RETAINED, NOT_RETAINED }

    enum class Direction { INPUT, OUTPUT }

    interface Type<N> {

        fun <T> accept(visitor: Visitor<T>): T

        val serializer: Serializer<N>

        interface Visitor<out T> {
            fun onVoid(type: Type<Unit>): T
            fun onBoolean(type: Type<Boolean>): T
            fun onFloat(type: Type<Float>): T
        }
    }

    fun <T> accept(visitor: Visitor<T>): T {
        return type.accept(BypassVisitor(visitor))
    }

    @Suppress("UNCHECKED_CAST")
    private inner class BypassVisitor<out T>(private val visitor: Visitor<T>) : Type.Visitor<T> {
        override fun onVoid(type: Type<Unit>): T = visitor.onVoid(this@Endpoint as Endpoint<Unit>)
        override fun onBoolean(type: Type<Boolean>): T = visitor.onBoolean(this@Endpoint as Endpoint<Boolean>)
        override fun onFloat(type: Type<Float>): T = visitor.onFloat(this@Endpoint as Endpoint<Float>)
    }

    interface Visitor<out T> {
        fun onVoid(endpoint: Endpoint<Unit>): T
        fun onBoolean(endpoint: Endpoint<Boolean>): T
        fun onFloat(endpoint: Endpoint<Float>): T
    }
}

enum class Units {
    NO,
    CELSIUS,
    OHMS,
    AMPS,
    VOLTS,
    SECONDS,
    MINUTES,
    DAYS,
    LITERS,
    KILOGRAMS,
    GRAMS,
    PPM,
    PERCENTS
}
