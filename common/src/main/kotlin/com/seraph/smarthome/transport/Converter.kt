package com.seraph.smarthome.transport

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
data class JsonConverter<T : Any>(private val clazz: KClass<T>) : TypedTopic.DataConverter<T> {

    private val gson = Gson()

    override fun fromString(string: String): T {
        try {
            return gson.fromJson(string, clazz.java)
        } catch (ex: JsonSyntaxException) {
            throw TypedTopic.DataConverter.TypeMismatchException(ex)
        }
    }

    override fun toString(data: T): String {
        return gson.toJson(data, clazz.java)
    }
}

class BooleanConverter : TypedTopic.DataConverter<Boolean> {
    override fun fromString(string: String): Boolean = when (string) {
        "true" -> true
        "false" -> false
        else -> throw TypedTopic.DataConverter.TypeMismatchException("Cannot convert $string to boolean")
    }

    override fun toString(data: Boolean): String = data.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

class ActionConverter : TypedTopic.DataConverter<Unit> {
    override fun fromString(string: String) = Unit
    override fun toString(data: Unit): String = System.currentTimeMillis().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}