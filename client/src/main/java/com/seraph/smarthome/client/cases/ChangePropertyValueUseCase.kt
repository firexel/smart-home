package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.Device
import com.seraph.smarthome.client.model.Property
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class ChangePropertyValueUseCase<T>(
        private val brokerRepo: BrokerRepo,
        private val deviceId: Device.Id,
        private val property: Property<T>,
        private val value: T)
    : UseCase<BrokerCredentials, Unit> {

    override fun execute(params: BrokerCredentials): Observable<Unit> {
        return brokerRepo.openConnection(params).flatMap { it.change(deviceId, property, value) }
    }
}