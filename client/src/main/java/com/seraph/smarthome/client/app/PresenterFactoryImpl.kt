package com.seraph.smarthome.client.app

import com.seraph.smarthome.client.presentation.*
import com.seraph.smarthome.client.view.PresenterFactory

class PresenterFactoryImpl(
        private val useCaseFactory: UseCaseFactory,
        private val navigator: Navigator
) : PresenterFactory {

    override fun createScenePresenter(view: ScenePresenter.View): ScenePresenter =
            ScenePresenterImpl(view, useCaseFactory, navigator)

    override
    fun createBrokersPresenter(view: BrokersPresenter.View): BrokersPresenter =
            BrokersPresenterImpl(view, useCaseFactory, navigator)

    override
    fun createNewBrokerPresenter(view: NewBrokerPresenter.View): NewBrokerPresenter =
            NewBrokerPresenterImpl(view, useCaseFactory, navigator)
}
