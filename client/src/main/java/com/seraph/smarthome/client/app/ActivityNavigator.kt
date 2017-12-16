package com.seraph.smarthome.client.app

import android.app.Activity
import android.content.Intent
import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.presentation.Navigator
import com.seraph.smarthome.client.view.NewBrokerActivity
import com.seraph.smarthome.client.view.SceneActivity

class ActivityNavigator(private val activity: Activity) : Navigator {

    override fun showSceneScreen(brokerSettings: BrokerSettings) {
        val intent = Intent(activity, SceneActivity::class.java)
        intent.putExtra("param", brokerSettings)
        activity.startActivity(intent)
    }

    override fun showNewBrokerSettingsScreen() {
        activity.startActivity(Intent(activity, NewBrokerActivity::class.java))
    }

    override fun showPreviousScreen() {
        activity.finish()
    }
}