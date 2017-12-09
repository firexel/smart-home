package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.app.Navigator
import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import io.reactivex.android.schedulers.AndroidSchedulers

class BrokersPresenterImpl(
        private val view: BrokersPresenter.View,
        private val repo: BrokersSettingsRepo,
        private val navigator: Navigator
) : BrokersPresenter {

    override fun onRefresh() {
        repo.getBrokersSettings()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.showBrokers(it.map(::toBrokerViewModel)) }
    }

    override fun onAddNewBroker() {
        navigator.showNewBrokerSettingsScreen()
    }

    override fun onBrokerSelected(broker: BrokersPresenter.BrokerViewModel) {
        repo.findBrokerSettings(broker.id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it != null) {
                        navigator.showDevicesScreen(it)
                    } else {
                        view.showError("Broker ${broker.id} not found")
                    }
                }
    }
}

private fun toBrokerViewModel(model: BrokerSettings) =
        BrokersPresenter.BrokerViewModel(model.id, "${model.host}:${model.port}")