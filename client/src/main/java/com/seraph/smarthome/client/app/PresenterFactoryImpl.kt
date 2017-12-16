package com.seraph.smarthome.client.app

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.presentation.*
import com.seraph.smarthome.client.view.PresenterFactory

class PresenterFactoryImpl(private val useCaseFactory: UseCaseFactory) : PresenterFactory {
    override fun createScenePresenter(view: ScenePresenter.View, settings:BrokerSettings): ScenePresenter =
            ScenePresenterImpl(view, useCaseFactory, settings)

    override
    fun createBrokersPresenter(view: BrokersPresenter.View, navigator: Navigator): BrokersPresenter =
            BrokersPresenterImpl(view, useCaseFactory, navigator)

    override
    fun createNewBrokerPresenter(view: NewBrokerPresenter.View, navigator: Navigator): NewBrokerPresenter =
            NewBrokerPresenterImpl(view, useCaseFactory, navigator)
}
