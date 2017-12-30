package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerInfo
import io.reactivex.android.schedulers.AndroidSchedulers

class BrokersPresenterImpl(
        private val view: BrokersPresenter.View,
        private val useCaseFactory: UseCaseFactory,
        private val navigator: Navigator
) : BrokersPresenter {

    private val infoMap: MutableMap<BrokersPresenter.BrokerViewModel, BrokerInfo> = mutableMapOf()

    override fun onRefresh() {
        useCaseFactory.listBrokersSettings().execute(Unit)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    infoMap.clear()
                    val vms = mutableListOf<BrokersPresenter.BrokerViewModel>()
                    it.forEach { info ->
                        val vm = BrokersPresenter.BrokerViewModel(
                                info.metadata.brokerName,
                                "${info.credentials.host}:${info.credentials.port}"
                        )
                        vms.add(vm)
                        infoMap.put(vm, info)
                    }
                    view.showBrokers(vms)
                }
    }

    override fun onAddNewBroker() {
        navigator.show(NewBrokerPresenter.NewBrokerScreen())
    }

    override fun onBrokerSelected(broker: BrokersPresenter.BrokerViewModel) {
        if (infoMap.contains(broker)) {
            navigator.show(ScenePresenter.SceneScreen(infoMap[broker]!!.credentials))
        } else {
            view.showError("Broker ${broker.name} not found")
        }
    }
}
