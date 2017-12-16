package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerSettings

interface Navigator {
    fun showDevicesScreen(brokerSettings: BrokerSettings)
    fun showNewBrokerSettingsScreen()
    fun showPreviousScreen()
}