package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.domain.Device
import io.reactivex.Observable

class ListDevicesUseCase(private val brokerRepo: BrokerRepo)
    : UseCase<BrokerCredentials, List<Device>> {

    override fun execute(params: BrokerCredentials): Observable<List<Device>> {
        return brokerRepo.openConnection(params).flatMap { it.devices }
    }
}