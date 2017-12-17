package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.model.Device
import io.reactivex.Observable

class ListDevicesUseCase(private val brokerRepo: BrokerRepo) : UseCase<BrokerCredentials, Collection<Device>> {
    override fun execute(params: BrokerCredentials): Observable<Collection<Device>> {
        return brokerRepo.openConnection(params)
                .flatMap { it.observeDevices() }
    }
}