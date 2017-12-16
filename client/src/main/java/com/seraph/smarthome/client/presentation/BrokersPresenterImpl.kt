package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerSettings
import io.reactivex.android.schedulers.AndroidSchedulers

class BrokersPresenterImpl(
        private val view: BrokersPresenter.View,
        private val useCaseFactory: UseCaseFactory,
        private val navigator: Navigator
) : BrokersPresenter {

    override fun onRefresh() {
        useCaseFactory.listBrokersSettings().execute(Unit)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.showBrokers(it.map(::toBrokerViewModel)) }
    }

    override fun onAddNewBroker() {
        navigator.showNewBrokerSettingsScreen()
    }

    override fun onBrokerSelected(broker: BrokersPresenter.BrokerViewModel) {
        useCaseFactory.findBrokeSettings().execute(broker.id)
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