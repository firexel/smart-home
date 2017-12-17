package com.seraph.smarthome.client.presentation

/**
 * Created by alex on 10.12.17.
 */
interface NewBrokerPresenter {

    fun onAddBroker(hostname: String, port: Int)

    interface View {
        fun showAddError(message: String)
    }

    class NewBrokerScreen : Screen {
        override fun <T> acceptVisitor(visitor: Screen.Visitor<T>): T =
                visitor.newBrokerScreenVisited()
    }
}