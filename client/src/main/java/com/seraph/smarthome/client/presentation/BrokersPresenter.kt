package com.seraph.smarthome.client.presentation

/**
 * Created by alex on 09.12.17.
 */
interface BrokersPresenter {

    fun onRefresh()

    fun onAddNewBroker()

    fun onBrokerSelected(broker: BrokerViewModel)

    interface View {
        fun showBrokers(brokers: List<BrokerViewModel>)
        fun showError(text: String)
    }

    data class BrokerViewModel(val name: String, val address:String)

    class BrokersListScreen : Screen {
        override fun <T> acceptVisitor(visitor: Screen.Visitor<T>): T =
                visitor.brokersListScreenVisited()
    }
}