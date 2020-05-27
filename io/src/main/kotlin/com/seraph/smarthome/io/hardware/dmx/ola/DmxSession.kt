package com.seraph.smarthome.io.hardware.dmx.ola

interface DmxSession {
    fun sendDmx(values: ShortArray)

    class SessionClosedException : RuntimeException()
}