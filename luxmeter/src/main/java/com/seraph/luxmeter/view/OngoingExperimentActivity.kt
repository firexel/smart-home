package com.seraph.luxmeter.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.seraph.luxmeter.R
import com.seraph.luxmeter.app.LuxMeterApp.Companion.presenters
import com.seraph.luxmeter.presenter.OngoingExperimentPresenter
import java.util.*
import kotlin.math.roundToInt

class OngoingExperimentActivity : AppCompatActivity(), OngoingExperimentPresenter.View {

    private lateinit var presenter: OngoingExperimentPresenter

    private lateinit var textLuxGauge: TextView
    private lateinit var textProgressGauge: TextView
    private lateinit var textPowerGauge: TextView
    private lateinit var textFileGauge: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_experiment_ongoing)
        textLuxGauge = findViewById(R.id.text_lux_gauge)
        textProgressGauge = findViewById(R.id.text_progress_gauge)
        textPowerGauge = findViewById(R.id.text_power_gauge)
        textFileGauge = findViewById(R.id.text_file_gauge)

        presenter = presenters.newOngoingExperimentPresenter(this)

        findViewById<Button>(R.id.button_stop).setOnClickListener {
            presenter.onExperimentStopped()
        }
    }

    override fun showProgress(progress: String) {
        textProgressGauge.text = progress
    }

    override fun showLuminance(luminance: Float) {
        textLuxGauge.text = "${luminance.roundToInt()}"
    }

    override fun showPower(powerLevel: Float) {
        textPowerGauge.text = "%.2f".format(Locale.ENGLISH, powerLevel * 100)
    }

    override fun showStorageInfo(storage: String) {
        textFileGauge.text = storage
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onExperimentStopped()
    }
}