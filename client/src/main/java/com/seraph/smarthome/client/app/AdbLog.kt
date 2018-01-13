package com.seraph.smarthome.client.app

import com.seraph.smarthome.util.Log

class AdbLog(private val component: String = "ClientApp") : Log {

    override fun copy(component: String): Log =
            AdbLog("${this.component}/$component")

    override fun i(message: String) {
        android.util.Log.i("ClientApp", message)
    }

    override fun w(message: String) {
        android.util.Log.w("ClientApp", message)
    }

    override fun v(message: String) {
        android.util.Log.v("ClientApp", message)
    }
}