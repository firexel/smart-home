package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.client.presentation.UseCaseFactory
import com.seraph.smarthome.model.Device

class ProductionUseCaseFactory(
        private val settingsRepo: BrokersSettingsRepo,
        private val brokerRepo: BrokerRepo) : UseCaseFactory {

    override fun addBroker(): UseCase<BrokerSettings, Unit> =
            AddBrokerSettingsUseCase(settingsRepo, brokerRepo)

    override fun listDevices(): UseCase<BrokerSettings, Collection<Device>> =
            ListDevicesUseCase(brokerRepo)

    override fun listBrokersSettings(): UseCase<Unit, List<BrokerSettings>> =
            ListBrokerSettingsUseCase(settingsRepo)

    override fun findBrokeSettings(): UseCase<Int, BrokerSettings?> =
            FindBrokerSettingsUseCase(settingsRepo)
}

