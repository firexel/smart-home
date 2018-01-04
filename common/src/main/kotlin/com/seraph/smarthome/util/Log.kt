package com.seraph.smarthome.util

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */
interface Log {
    fun i(message: String)
    fun w(message: String)

    fun copy(component: String): Log
}

class ConsoleLog(private val component: String = "") : Log {
    override fun copy(component: String) =
            if (component.isEmpty()) ConsoleLog(component)
            else ConsoleLog("${this.component}/$component")

    override fun i(message: String) {
        println("I $component: $message")
    }

    override fun w(message: String) {
        println("W $component: $message")
    }
}

class NoLog : Log {
    override fun copy(component: String): Log = this
    override fun i(message: String) = Unit
    override fun w(message: String) = Unit
}