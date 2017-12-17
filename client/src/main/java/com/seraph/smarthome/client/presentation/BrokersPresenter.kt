package com.seraph.smarthome.client.presentation

/**
 * Created by alex on 09.12.17.
 */
public interface BrokersPresenter {

    fun onRefresh()

    fun onAddNewBroker()

    fun onBrokerSelected(broker: BrokerViewModel)

    public interface View {
        fun showBrokers(brokers: List<BrokerViewModel>)
        fun showError(text: String)
    }

    public data class BrokerViewModel(val name: String, val address:String)
}