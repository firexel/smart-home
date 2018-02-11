package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.BrokerInfo
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
interface UseCaseFactory {

    fun addBroker(): UseCase<BrokerCredentials, Unit>

    fun listBrokersSettings(): UseCase<Unit, List<BrokerInfo>>

    fun observeBrokerMetainfo(): UseCase<BrokerCredentials, Metainfo>

    fun observeDevices(): UseCase<BrokerCredentials, List<Device>>

    fun observeConnectionState(): UseCase<BrokerCredentials, BrokerConnection.State>

    fun <T> publishEndpoint(deviceId: Device.Id, endpoint: Endpoint<T>, value: T): UseCase<BrokerCredentials, Unit>

    fun <T> observeEndpoint(deviceId: Device.Id, endpoint: Endpoint<T>): UseCase<BrokerCredentials, T>
}