package com.seraph.smarthome.client.app

import android.content.Context
import com.seraph.smarthome.client.presentation.BrokersPresenter
import com.seraph.smarthome.client.presentation.BrokersPresenterImpl
import com.seraph.smarthome.client.presentation.NewBrokerPresenter
import com.seraph.smarthome.client.presentation.NewBrokerPresenterImpl

class PresenterFactory(context: Context) {

    companion object {
        fun from(context:Context) = (context.applicationContext as ClientApp).presenterFactory
    }

    private val brokersRepo = DatabaseBrokersRepo(context)

    fun createBrokersPresenter(view: BrokersPresenter.View, navigator: Navigator): BrokersPresenter =
            BrokersPresenterImpl(view, brokersRepo, navigator)

    fun createNewBrokerPresenter(view: NewBrokerPresenter.View, navigator: Navigator): NewBrokerPresenter =
            NewBrokerPresenterImpl(view, brokersRepo, navigator)
}

