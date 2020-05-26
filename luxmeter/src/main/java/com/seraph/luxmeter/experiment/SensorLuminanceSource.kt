package com.seraph.luxmeter.experiment

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorLuminanceSource(activity: Activity) : LuminanceSource, SensorEventListener {

    private var luminanceLevel = 0f
    private var sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    init {
        val luminance = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensorManager.registerListener(this, luminance, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ignore
    }

    override fun onSensorChanged(event: SensorEvent?) {
        luminanceLevel = event!!.values[0]
    }

    override fun getLuminance(): Float {
        return luminanceLevel
    }

    override fun close() {
        sensorManager.unregisterListener(this)
    }
}