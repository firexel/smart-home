package com.seraph.smarthome.domain

import java.util.*

/**
 * Created by aleksandr.naumov on 20.01.18.
 */

class Types {
    companion object {
        val FLOAT = object : Endpoint.Type<Float> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onFloat(this)
            override val serializer: Serializer<Float> = FloatConverter()
            override fun cast(obj: Any): Float = obj as Float
        }

        val INTEGER = object : Endpoint.Type<Int> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onInt(this)
            override val serializer: Serializer<Int> = IntConverter()
            override fun cast(obj: Any): Int = obj as Int
        }

        val BOOLEAN = object : Endpoint.Type<Boolean> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onBoolean(this)
            override val serializer: Serializer<Boolean> = BooleanConverter()
            override fun cast(obj: Any): Boolean = obj as Boolean
        }

        val ACTION = object : Endpoint.Type<Int> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onAction(this)
            override val serializer: Serializer<Int> = ActionConverter()
            override fun cast(obj: Any) = obj as Int
        }

        val DEVICE_STATE = object : Endpoint.Type<DeviceState> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onDeviceState(this)
            override val serializer: Serializer<DeviceState> = DeviceStateConverter()
            override fun cast(obj: Any): DeviceState = obj as DeviceState
        }

        fun newActionId(): Int {
            val time = System.currentTimeMillis()
            return (time and (time shr 4 * 8)).toInt()
        }
    }
}

enum class DeviceState {
    OFFLINE,
    MALFUNCTION,
    DEGRADED,
    ONLINE
}

abstract class Serializer<T> {
    abstract fun fromBytes(bytes: ByteArray): T
    abstract fun toBytes(data: T): ByteArray

    class TypeMismatchException(msg: String, rootCause: Throwable)
        : RuntimeException(msg, rootCause) {

        constructor(msg: String) : this(msg, RuntimeException())
        constructor(rootCause: Throwable) : this("", rootCause)
    }
}

internal abstract class BaseStringConverter<T> : Serializer<T>() {
    override final fun fromBytes(bytes: ByteArray): T = fromString(String(bytes, Charsets.UTF_8))

    override final fun toBytes(data: T): ByteArray = toString(data).toByteArray(Charsets.UTF_8)

    abstract fun fromString(string: String): T
    abstract fun toString(data: T): String
}

internal class BooleanConverter : BaseStringConverter<Boolean>() {
    override fun fromString(string: String): Boolean = when (string) {
        "true" -> true
        "false" -> false
        else -> throw Serializer.TypeMismatchException("Unknown boolean $string")
    }

    override fun toString(data: Boolean): String = data.toString()
}

internal class FloatConverter : BaseStringConverter<Float>() {
    override fun fromString(string: String): Float {
        try {
            return string.toFloat()
        } catch (ex: NumberFormatException) {
            throw Serializer.TypeMismatchException("Unknown float $string", ex)
        }
    }

    override fun toString(data: Float): String = String.format(Locale.ENGLISH, "%10.100f", data)
}

internal class IntConverter : BaseStringConverter<Int>() {
    override fun fromString(string: String): Int {
        try {
            return string.toInt(10)
        } catch (ex: NumberFormatException) {
            throw Serializer.TypeMismatchException("Unknown int $string", ex)
        }
    }

    override fun toString(data: Int): String = String.format("%d", data)
}

internal class DeviceStateConverter : BaseStringConverter<DeviceState>() {
    override fun fromString(string: String): DeviceState {
        try {
            return DeviceState.valueOf(string)
        } catch (ex: IllegalArgumentException) {
            throw Serializer.TypeMismatchException("Unknown device state $string", ex)
        }
    }

    override fun toString(data: DeviceState): String = data.name
}

internal class ActionConverter : BaseStringConverter<Int>() {
    override fun fromString(string: String): Int {
        try {
            return string.toInt(10)
        } catch (ex: NumberFormatException) {
            throw TypeMismatchException("Unknown action id $string", ex)
        }
    }

    override fun toString(data: Int): String = String.format("%d", data)
}