package com.seraph.luxmeter.app

import com.seraph.smarthome.util.Log

class AdbLog(private val component: String = "LuxmeterApp") : Log {

    override fun copy(component: String): Log =
            AdbLog("${this.component}/$component")

    override fun i(message: String) {
        android.util.Log.i("LuxmeterApp", message)
    }

    override fun w(message: String) {
        android.util.Log.w("LuxmeterApp", message)
    }

    override fun v(message: String) {
        android.util.Log.v("LuxmeterApp", message)
    }
}