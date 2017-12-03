package com.seraph.smarthome.model

/**
 * Created by aleksandr.naumov on 03.12.2017.
 */
interface Log {
    fun i(message:String)
    fun w(message:String)
}

class ConsoleLog : Log {
    override fun i(message: String) {
        println("I: $message")
    }

    override fun w(message: String) {
        println("W: $message")
    }
}