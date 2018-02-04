package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.BrokerInfo
import com.seraph.smarthome.client.model.BrokersInfoRepo
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.client.presentation.UseCaseFactory
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo

class ProductionUseCaseFactory(
        private val infoRepo: BrokersInfoRepo,
        private val brokerRepo: BrokerRepo)
    : UseCaseFactory {

    override fun addBroker(): UseCase<BrokerCredentials, Unit> =
            AddBrokerSettingsUseCase(infoRepo, brokerRepo)

    override fun observeDevices(): UseCase<BrokerCredentials, List<Device>> =
            ListDevicesUseCase(brokerRepo)

    override fun listBrokersSettings(): UseCase<Unit, List<BrokerInfo>> =
            ListBrokerInfoUseCase(infoRepo)

    override fun observeConnectionState()
            : UseCase<BrokerCredentials, BrokerConnection.State>
            = ObserveConnectionState(brokerRepo)

    override fun observeBrokerMetainfo()
            : UseCase<BrokerCredentials, Metainfo>
            = GetBrokerMetadataUseCase(brokerRepo)

    override fun <T> publishEndpoint(deviceId: Device.Id, endpoint: Endpoint<T>, value: T)
            : UseCase<BrokerCredentials, Unit>
            = PublishEndpointValueUseCase(brokerRepo, deviceId, endpoint, value)

    override fun <T> subscribeEndpoint(deviceId: Device.Id, endpoint: Endpoint<T>)
            : UseCase<BrokerCredentials, T>
            = throw IllegalStateException()
}

