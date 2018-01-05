package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

class ObserveConnectionState(private val brokerRepo: BrokerRepo)
    : UseCase<BrokerCredentials, BrokerConnection.State> {

    override fun execute(params: BrokerCredentials): Observable<BrokerConnection.State> {
        return brokerRepo.openConnection(params).flatMap { it.state }
    }
}