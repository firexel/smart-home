package com.seraph.smarthome.client.app

import android.app.Activity
import android.content.Intent
import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.view.NewBrokerActivity

class MockBrokersNavigator(private val activity: Activity) : Navigator {

    override fun showDevicesScreen(brokerSettings: BrokerSettings) {
    }

    override fun showNewBrokerSettingsScreen() {
        activity.startActivity(Intent(activity, NewBrokerActivity::class.java))
    }

    override fun showPreviousScreen() {
        activity.finish()
    }
}