package com.seraph.smarthome.client.app

import com.seraph.smarthome.util.Log

class AdbLog(private val component: String = "ClientApp") : Log {

    override fun copy(component: String): Log =
            AdbLog("${this.component}/$component")

    override fun i(message: String) {
        android.util.Log.i(component, message)
    }

    override fun w(message: String) {
        android.util.Log.w(component, message)
    }

    override fun v(message: String) {
        android.util.Log.v(component, message)
    }
}