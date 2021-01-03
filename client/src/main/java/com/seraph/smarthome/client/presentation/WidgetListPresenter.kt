package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.WidgetGroupModel
import ru.mail.march.channel.DataChannel

interface WidgetListPresenter {

    val widgets: DataChannel<List<WidgetGroupModel>>
    val state: DataChannel<ConnectionState>

    enum class ConnectionState {
        CONNECTING, CONNECTED
    }
}