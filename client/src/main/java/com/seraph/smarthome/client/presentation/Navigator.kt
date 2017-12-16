package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerSettings

interface Navigator {
    fun showSceneScreen(brokerSettings: BrokerSettings)
    fun showNewBrokerSettingsScreen()
    fun showPreviousScreen()
}