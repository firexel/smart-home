package com.seraph.smarthome.client.view

import com.seraph.smarthome.client.presentation.BrokersPresenter
import com.seraph.smarthome.client.presentation.NewBrokerPresenter
import com.seraph.smarthome.client.presentation.ScenePresenter

interface PresenterFactory {
    fun createBrokersPresenter(view: BrokersPresenter.View): BrokersPresenter
    fun createNewBrokerPresenter(view: NewBrokerPresenter.View): NewBrokerPresenter
    fun createScenePresenter(view: ScenePresenter.View): ScenePresenter
}