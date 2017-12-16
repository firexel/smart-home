package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.model.Device
import io.reactivex.Observable

class ListDevicesUseCase(private val brokerRepo: BrokerRepo) : UseCase<BrokerSettings, Collection<Device>> {
    override fun execute(params: BrokerSettings): Observable<Collection<Device>> {
        return brokerRepo.openConnection(params)
                .flatMap { it.observeDevices() }
    }
}