package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.BrokerInfo
import com.seraph.smarthome.client.model.BrokersInfoRepo
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.client.presentation.UseCaseFactory
import com.seraph.smarthome.model.Device

class ProductionUseCaseFactory(
        private val infoRepo: BrokersInfoRepo,
        private val brokerRepo: BrokerRepo) : UseCaseFactory {

    override fun addBroker(): UseCase<BrokerCredentials, Unit> =
            AddBrokerSettingsUseCase(infoRepo, brokerRepo)

    override fun listDevices(): UseCase<BrokerCredentials, Collection<Device>> =
            ListDevicesUseCase(brokerRepo)

    override fun listBrokersSettings(): UseCase<Unit, List<BrokerInfo>> =
            ListBrokerInfoUseCase(infoRepo)

}

