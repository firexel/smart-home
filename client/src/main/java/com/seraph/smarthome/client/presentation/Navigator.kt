package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerCredentials

interface Navigator {
    fun showSceneScreen(credentials: BrokerCredentials)
    fun showNewBrokerSettingsScreen()
    fun showPreviousScreen()
}