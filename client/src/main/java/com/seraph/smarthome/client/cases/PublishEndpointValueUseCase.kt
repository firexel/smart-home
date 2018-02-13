package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 29.12.17.
 */
class PublishEndpointValueUseCase<T>(
        private val brokerRepo: BrokerRepo,
        private val deviceId: Device.Id,
        private val endpoint: Endpoint<T>,
        private val value: T)
    : UseCase<BrokerCredentials, Unit> {

    override fun execute(params: BrokerCredentials): Observable<Unit> {
        return brokerRepo.openConnection(params).flatMap { it.publish(deviceId, endpoint, value) }
    }
}