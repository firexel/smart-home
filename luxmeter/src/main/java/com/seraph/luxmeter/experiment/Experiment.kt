package com.seraph.luxmeter.experiment

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.*

class LuminanceExperiment(
        private val luminanceSource: LuminanceSource,
        private val powerSetter: PowerSetter,
        private val resultLogger: ResultLogger
) {

    private val listeners = mutableListOf<Listener>()
    private val handler: Handler
    private val uiHandler: Handler = Handler(Looper.getMainLooper())
    private val probes: Queue<Float> = LinkedList()
    private var probesTotal: Int = 0

    init {
        val thread = HandlerThread("Experiment")
        thread.start()
        handler = Handler(thread.looper)
    }

    fun addListener(listener: Listener) = handler.post {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) = handler.post {
        listeners.remove(listener)
    }

    fun start() {
        init()
        step()
    }

    private fun init() = handler.post {
        probes.clear()
        probes.addAll(((0..200) + (199 downTo 0)).map { it / 200f })
        probesTotal = probes.size
    }

    private fun step() {
        handler.post {
            if (probes.isNotEmpty()) {
                val probe = probes.poll()
                powerSetter.setPower(probe)
                Thread.sleep(2000)
                val luminance = luminanceSource.getLuminance()
                resultLogger.logParameters(probe, luminance)
                listeners.forEach {
                    val progress = (probesTotal - probes.size) / probesTotal.toFloat()
                    uiHandler.post { it.onAdvanced(probe, luminance, progress) }
                }
            }
            if (probes.isNotEmpty()) {
                step()
            } else {
                resultLogger.close()
                luminanceSource.close()
                listeners.forEach { uiHandler.post { it.onFinished() } }
            }
        }
    }

    fun stop() {
        handler.post {
            probes.clear()
        }
    }

    fun getStorageInfo(): String {
        return resultLogger.getStorageInfo()
    }
}

interface LuminanceSource {
    fun getLuminance(): Float
    fun close()
}

interface PowerSetter {
    fun setPower(powerLevel: Float)
}

interface ResultLogger {
    fun logParameters(powerLevel: Float, luminance: Float)
    fun close()
    fun getStorageInfo(): String
}

interface Listener {
    fun onAdvanced(powerLevel: Float, luminance: Float, progress: Float)
    fun onFinished()
}
