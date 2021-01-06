package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.app.Services
import com.seraph.smarthome.client.interactors.WidgetListInteractor
import com.seraph.smarthome.client.model.WidgetGroupModel
import ru.mail.march.channel.DataChannel
import ru.mail.march.interactor.InteractorObtainer

class WidgetListPresenterImpl(obtainer: InteractorObtainer, services: Services) : WidgetListPresenter {

    override lateinit var widgets: DataChannel<List<WidgetGroupModel>>
    override lateinit var state: DataChannel<WidgetListPresenter.ConnectionState>

    init {
        val widgetListInteractor = obtainer.obtain(WidgetListInteractor::class.java) {
            WidgetListInteractor(services.networkRepository, services.log.copy("WidgetListInteractor"))
        }
        widgets = widgetListInteractor.widgets
    }
}