package com.seraph.smarthome.domain.impl

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.seraph.smarthome.domain.BaseStringConverter
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 20.01.18.
 */
internal class JsonSerializer<T : Any>(
        private val gson: Gson,
        private val klass: KClass<T>
) : BaseStringConverter<T>() {

    override fun fromString(string: String): T {
        try {
            return gson.fromJson(string, klass.java)
        } catch (ex: JsonSyntaxException) {
            throw TypeMismatchException("Cannot read class $klass from json string $string", ex)
        }
    }

    override fun toString(data: T): String = gson.toJson(data)
}