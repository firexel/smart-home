package com.seraph.smarthome.io


/**
 * Created by aleksandr.naumov on 24.12.17.
 */
interface IoDevice {
    val sensorsTotal: Int
    val relaysTotal: Int

    fun getSensorsState(): BooleanArray
    fun setRelayState(relayIndex: Int, enable: Boolean)
}