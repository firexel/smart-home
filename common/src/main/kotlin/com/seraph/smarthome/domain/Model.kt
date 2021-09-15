package com.seraph.smarthome.domain

import kotlinx.serialization.Serializable


/**
 * Created by aleksandr.naumov on 29.12.17.
 */

@Serializable
data class Metainfo(
        val brokerName: String,
        val role: Role,
        val widgetGroups: List<WidgetGroup>
) {
    enum class Role {
        USER, ADMIN
    }
}

data class Device(
        val id: Id,
        val endpoints: List<Endpoint<*>> = emptyList()
) {
    data class Id(val segments: List<String>) {
        constructor(vararg segments: String) : this(segments.toList())

        val value: String = segments.joinToString(":")
        override fun toString(): String = value
        fun innerId(newSegment: String) = Id(segments + newSegment)
    }
}

data class Endpoint<N>(
        val id: Id,
        val type: Type<N>,
        val direction: Direction,
        val retention: Retention,
        val dataKind: DataKind,
        val interaction: Interaction,
        val units: Units = Units.NO
) {
    data class Id(val value: String) {
        override fun toString(): String = value
    }

    enum class Retention { NOT_RETAINED, RETAINED }

    enum class Direction { INPUT, OUTPUT }

    enum class DataKind {
        CURRENT, // averages during compaction
        CUMULATIVE, // maxing during compaction
        EVENT // sums during compaction
    }

    enum class Interaction {
        MAIN, // main goal of a device to read or show this data
        USER_EDITABLE, // useful but not main controls of a device
        USER_READONLY, // useful but dangerous to change values
        INVISIBLE // values which should not be shown or user-modified
    }

    interface Type<N> {

        fun <T> accept(visitor: Visitor<T>): T
        fun cast(obj: Any): N

        val serializer: Serializer<N>

        interface Visitor<out T> {
            fun onAction(type: Type<Int>): T
            fun onBoolean(type: Type<Boolean>): T
            fun onFloat(type: Type<Float>): T
            fun onInt(type: Type<Int>): T
            fun onDeviceState(type: Type<DeviceState>): T
        }

        open class DefaultVisitor<out T>(private val defaultValue: T) : Visitor<T> {
            override fun onInt(type: Type<Int>): T = defaultValue
            override fun onAction(type: Type<Int>): T = defaultValue
            override fun onBoolean(type: Type<Boolean>): T = defaultValue
            override fun onFloat(type: Type<Float>): T = defaultValue
            override fun onDeviceState(type: Type<DeviceState>): T = defaultValue
        }
    }

    fun <T> accept(visitor: Visitor<T>): T {
        return type.accept(BypassVisitor(visitor))
    }

    fun cast(obj: Any): N {
        return type.cast(obj)
    }

    @Suppress("UNCHECKED_CAST")
    private inner class BypassVisitor<out T>(private val visitor: Visitor<T>) : Type.Visitor<T> {
        override fun onInt(type: Type<Int>): T = visitor.onInt(this@Endpoint as Endpoint<Int>)
        override fun onAction(type: Type<Int>): T = visitor.onAction(this@Endpoint as Endpoint<Int>)
        override fun onBoolean(type: Type<Boolean>): T = visitor.onBoolean(this@Endpoint as Endpoint<Boolean>)
        override fun onFloat(type: Type<Float>): T = visitor.onFloat(this@Endpoint as Endpoint<Float>)
        override fun onDeviceState(type: Type<DeviceState>): T = visitor.onDeviceState(this@Endpoint as Endpoint<DeviceState>)
    }

    interface Visitor<out T> {
        fun onInt(endpoint: Endpoint<Int>): T
        fun onAction(endpoint: Endpoint<Int>): T
        fun onBoolean(endpoint: Endpoint<Boolean>): T
        fun onFloat(endpoint: Endpoint<Float>): T
        fun onDeviceState(endpoint: Endpoint<DeviceState>): T
    }
}

enum class Units {
    NO,
    CELSIUS,
    PPM,
    PPB,
    PERCENTS_0_1,
    LX,
    ON_OFF,
    W, // watts
    KWH, // kWh
    V // volts
}
