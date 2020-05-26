package com.seraph.luxmeter.presenter

import android.app.Activity
import com.seraph.luxmeter.app.LuxMeterApp.Companion.app
import com.seraph.smarthome.transport.Broker

class ExperimentSettingsPresenter(
        private val view: View,
        private val broker: Broker,
        private val activity: Activity) {

    private val stateListener = object : Broker.StateListener {
        override fun onStateChanged(brokerState: Broker.BrokerState) {
            val stateName = brokerState.accept(object : Broker.BrokerState.Visitor<String> {
                override fun onConnectedState(): String = "OK"
                override fun onDisconnectedState(): String = "Disconnected"
                override fun onDisconnectingState(): String = "Disconnecting..."
                override fun onWaitingState(msToReconnect: Long): String = "Waiting..."
                override fun onConnectingState(): String = "Connecting..."
            })
            view.showConnectionStatus(stateName)
        }
    }

    init {
        view.showConnectionStatus("Idle")
        broker.addStateListener(stateListener)
    }

    interface View {
        fun showConnectionStatus(status: String)
        fun showError(error: String)
    }

    fun onExperimentStart(endpoint: String) {
        broker.removeStateListener(stateListener)
        try {
            activity.app.beginExperiment(endpoint, activity)
            activity.app.goToOngoingExperimentScreen()
        } catch (e: Exception) {
            view.showError(e.message + "")
        }
    }
}