package com.seraph.smarthome.client.presentation

import android.support.v7.util.DiffUtil
import com.seraph.smarthome.client.model.BrokerCredentials

interface ScenePresenter {

    fun onDeviceActionPerformed(deviceId: String, actionId: String)

    interface View {
        fun onShowDevices(devices: List<DeviceViewModel>, diff: DiffUtil.DiffResult)
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