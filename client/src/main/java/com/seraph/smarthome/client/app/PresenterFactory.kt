package com.seraph.smarthome.client.app

import android.app.Activity
import com.seraph.smarthome.client.presentation.BrokersPresenter
import com.seraph.smarthome.client.presentation.BrokersPresenterImpl
import com.seraph.smarthome.client.presentation.NewBrokerPresenter
import com.seraph.smarthome.client.presentation.NewBrokerPresenterImpl

class PresenterFactory(private val activity: Activity) {
    fun createBrokersPresenter(view: BrokersPresenter.View): BrokersPresenter =
            BrokersPresenterImpl(view, DatabaseBrokersRepo(activity), MockBrokersNavigator(activity))

    fun createNewBrokerPresenter(view: NewBrokerPresenter.View): NewBrokerPresenter =
            NewBrokerPresenterImpl(view, DatabaseBrokersRepo(activity), MockBrokersNavigator(activity))
}

