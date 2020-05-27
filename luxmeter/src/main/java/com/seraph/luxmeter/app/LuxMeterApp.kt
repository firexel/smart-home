package com.seraph.luxmeter.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import com.seraph.luxmeter.experiment.*
import com.seraph.luxmeter.presenter.ExperimentSettingsPresenter
import com.seraph.luxmeter.presenter.OngoingExperimentPresenter
import com.seraph.luxmeter.view.OngoingExperimentActivity
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.impl.Brokers

class LuxMeterApp : Application(), Listener {

    private lateinit var broker: Broker
    private var experiment: LuminanceExperiment? = null

    companion object {
        val Context.app: LuxMeterApp
            get() = applicationContext as LuxMeterApp


        val Activity.presenters: PresenterFactory
            get() = app.obtainPresentersFactory(this)
    }

    private fun obtainPresentersFactory(activity: Activity): PresenterFactory {
        return ProductionPresenterFactory(activity)
    }

    override fun onCreate() {
        super.onCreate()

        broker = Brokers.unencrypted("tcp://vinchi:1883", "luxmeter", AdbLog())
    }

    fun beginExperiment(globalEndpoint: String, activity: Activity): LuminanceExperiment {
        if (experiment == null) {
            experiment = LuminanceExperiment(
                    SensorLuminanceSource(activity),
                    BrokerPowerSetter(broker, globalEndpoint),
                    FileLogger(activity.getExternalFilesDir(null)!!, "experiment")
            ).also {
                it.addListener(this)
                it.start()
            }
        }
        return experiment!!
    }

    fun goToOngoingExperimentScreen() {
        startActivity(Intent(this, OngoingExperimentActivity::class.java))
    }

    override fun onAdvanced(powerLevel: Float, luminance: Float, progress: Float) = Unit

    override fun onFinished() {
        experiment?.removeListener(this)
        experiment = null
    }

    private inner class ProductionPresenterFactory(private val activity: Activity) : PresenterFactory {

        override fun newOngoingExperimentPresenter(view: OngoingExperimentPresenter.View)
                : OngoingExperimentPresenter {

            return OngoingExperimentPresenter(view, experiment!!)
        }

        override fun newExperimentSettingsPresenter(view: ExperimentSettingsPresenter.View)
                : ExperimentSettingsPresenter {

            return ExperimentSettingsPresenter(view, broker, activity)
        }
    }
}

