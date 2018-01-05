package com.seraph.smarthome.client.presentation

import android.content.Context
import com.seraph.smarthome.client.app.ClientApp
import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.client.model.*

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
interface UseCaseFactory {

    companion object {
        fun from(context: Context) = (context.applicationContext as ClientApp).useCaseFactory
    }

    fun addBroker(): UseCase<BrokerCredentials, Unit>

    fun listBrokersSettings(): UseCase<Unit, List<BrokerInfo>>

    fun observeBrokerMetadata(): UseCase<BrokerCredentials, Metadata>

    fun listDevices(): UseCase<BrokerCredentials, List<Device>>

    fun observeConnectionState(): UseCase<BrokerCredentials, BrokerConnection.State>

    fun <T> changePropertyValue(deviceId: Device.Id, property: Property<T>, value: T)
            : UseCase<BrokerCredentials, Unit>
}