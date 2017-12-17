package com.seraph.smarthome.client.view

import android.content.Context
import com.seraph.smarthome.client.app.ClientApp
import com.seraph.smarthome.client.presentation.BrokersPresenter
import com.seraph.smarthome.client.presentation.Navigator
import com.seraph.smarthome.client.presentation.NewBrokerPresenter
import com.seraph.smarthome.client.presentation.ScenePresenter

interface PresenterFactory {

    companion object {
        fun from(context: Context) = (context.applicationContext as ClientApp).presenterFactory
    }

    fun createBrokersPresenter(view: BrokersPresenter.View, navigator: Navigator): BrokersPresenter
    fun createNewBrokerPresenter(view: NewBrokerPresenter.View, navigator: Navigator): NewBrokerPresenter
    fun createScenePresenter(view: ScenePresenter.View, navigator: Navigator): ScenePresenter
}