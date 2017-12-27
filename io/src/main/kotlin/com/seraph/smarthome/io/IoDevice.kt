package com.seraph.smarthome.io


/**
 * Created by aleksandr.naumov on 24.12.17.
 */
interface IoDevice {
    val inputsTotal: Int
    val outputsTotal: Int

    fun getInputsState(): BooleanArray
    fun setOutputState(outputIndex: Int, enable: Boolean)
}