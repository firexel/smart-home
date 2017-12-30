package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.*
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.client.presentation.UseCaseFactory

class ProductionUseCaseFactory(
        private val infoRepo: BrokersInfoRepo,
        private val brokerRepo: BrokerRepo)
    : UseCaseFactory {

    override fun addBroker(): UseCase<BrokerCredentials, Unit> =
            AddBrokerSettingsUseCase(infoRepo, brokerRepo)

    override fun listDevices(): UseCase<BrokerCredentials, List<Device>> =
            ListDevicesUseCase(brokerRepo)

    override fun listBrokersSettings(): UseCase<Unit, List<BrokerInfo>> =
            ListBrokerInfoUseCase(infoRepo)

    override fun <T> changePropertyValue(deviceId: Device.Id, property: Property<T>, value: T)
            : UseCase<BrokerCredentials, Unit>
            = ChangePropertyValueUseCase(brokerRepo, deviceId, property, value)
}

