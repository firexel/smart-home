package com.seraph.luxmeter.presenter

import com.seraph.luxmeter.experiment.Listener
import com.seraph.luxmeter.experiment.LuminanceExperiment
import kotlin.math.roundToInt

class OngoingExperimentPresenter(
        private val view: View,
        private val experiment: LuminanceExperiment) : Listener {

    init {
        view.showStorageInfo(experiment.getStorageInfo())
        experiment.addListener(this)
    }

    fun onExperimentStopped() {
        experiment.stop()
    }

    override fun onAdvanced(powerLevel: Float, luminance: Float, progress: Float) {
        view.showLuminance(luminance)
        view.showPower(powerLevel)
        view.showProgress("${(progress * 100).roundToInt()}")
    }

    override fun onFinished() {
        view.showProgress("Finished")
    }

    interface View {
        fun showProgress(progress: String)
        fun showLuminance(luminance: Float)
        fun showPower(powerLevel: Float)
        fun showStorageInfo(storage:String)
    }
}