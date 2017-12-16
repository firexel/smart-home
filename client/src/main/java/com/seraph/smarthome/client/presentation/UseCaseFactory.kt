package com.seraph.smarthome.client.presentation

import android.content.Context
import com.seraph.smarthome.client.app.ClientApp
import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.model.Device

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
interface UseCaseFactory {

    companion object {
        fun from(context: Context) = (context.applicationContext as ClientApp).useCaseFactory
    }

    fun addBroker(): UseCase<BrokerSettings, Unit>
    fun listBrokersSettings(): UseCase<Unit, List<BrokerSettings>>
    fun findBrokeSettings(): UseCase<Int, BrokerSettings?>

    fun listDevices(): UseCase<BrokerSettings, Collection<Device>>
}