package com.seraph.smarthome.client.presentation

/**
 * Created by alex on 10.12.17.
 */
public interface NewBrokerPresenter {

    fun onAddBroker(hostname: String, port: Int)

    public interface View {
        fun showAddError(message:String)
    }
}