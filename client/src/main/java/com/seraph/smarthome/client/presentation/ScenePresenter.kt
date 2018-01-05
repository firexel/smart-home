package com.seraph.smarthome.client.presentation

import android.support.v7.util.DiffUtil
import com.seraph.smarthome.client.model.BrokerCredentials

interface ScenePresenter {

    fun onDeviceActionPerformed(deviceId: String, actionId: String)
    fun onGoingBack()

    interface View {
        fun showDevices(devices: List<DeviceViewModel>, diff: DiffUtil.DiffResult)
        fun showConnectionStatus(status: String)
        fun showBrokerName(name: String)
    }

    data class DeviceViewModel(
            val id: String,
            val name: String,
            val mainActionId: String?,
            val mainIndicatorValue: Boolean?
    )

    class SceneScreen(val credentials: BrokerCredentials) : Screen {
        override fun <T> acceptVisitor(visitor: Screen.Visitor<T>): T = visitor.sceneScreenVisited()
    }
}