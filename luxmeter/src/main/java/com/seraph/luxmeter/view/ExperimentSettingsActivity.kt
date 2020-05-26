package com.seraph.luxmeter.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.seraph.luxmeter.R
import com.seraph.luxmeter.app.LuxMeterApp.Companion.presenters
import com.seraph.luxmeter.presenter.ExperimentSettingsPresenter

class ExperimentSettingsActivity : AppCompatActivity(), ExperimentSettingsPresenter.View {

    private lateinit var editEndpoint: EditText
    private lateinit var textConnectionStatus: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_experiment_settings)
        editEndpoint = findViewById(R.id.text_endpoint_gauge)
        textConnectionStatus = findViewById(R.id.text_connection_gauge)

        val presenter = presenters.newExperimentSettingsPresenter(this)

        findViewById<Button>(R.id.button_start).setOnClickListener {
            presenter.onExperimentStart(editEndpoint.text.toString())
        }
    }

    override fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    override fun showConnectionStatus(status: String) {
        textConnectionStatus.text = status
    }
}