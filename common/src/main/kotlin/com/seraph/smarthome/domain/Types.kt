package com.seraph.smarthome.domain

/**
 * Created by aleksandr.naumov on 20.01.18.
 */

class Types {
    companion object {
        val FLOAT = object : Endpoint.Type<Float> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onFloat(this)
            override val serializer: Serializer<Float> = FloatConverter()
        }

        val BOOLEAN = object : Endpoint.Type<Boolean> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onBoolean(this)
            override val serializer: Serializer<Boolean> = BooleanConverter()
        }

        val VOID = object : Endpoint.Type<Unit> {
            override fun <T> accept(visitor: Endpoint.Type.Visitor<T>): T = visitor.onVoid(this)
            override val serializer: Serializer<Unit> = VoidConverter()
        }
    }
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
    override final fun fromBytes(bytes: ByteArray): T
            = fromString(String(bytes, Charsets.UTF_8))

    override final fun toBytes(data: T): ByteArray
            = toString(data).toByteArray(Charsets.UTF_8)

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

    override fun toString(data: Float): String = String.format("%10.1f", data)
}

internal class VoidConverter : BaseStringConverter<Unit>() {
    override fun fromString(string: String) = when (string) {
        "*" -> Unit
        else -> throw Serializer.TypeMismatchException("Unknown unit $string")
    }

    override fun toString(data: Unit): String = "*"
}