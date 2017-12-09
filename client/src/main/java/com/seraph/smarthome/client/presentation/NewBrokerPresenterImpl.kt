package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.app.Navigator
import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import io.reactivex.android.schedulers.AndroidSchedulers

class NewBrokerPresenterImpl(
        private val view: NewBrokerPresenter.View,
        private val brokersRepo: BrokersSettingsRepo,
        private val navigator: Navigator
) : NewBrokerPresenter {

    override fun onAddBroker(hostname: String, port: Int) {
        brokersRepo.saveBrokerSettings(BrokerSettings(host = hostname, port = port))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { view.showAddError() }
                .subscribe { navigator.showPreviousScreen() }
    }
}