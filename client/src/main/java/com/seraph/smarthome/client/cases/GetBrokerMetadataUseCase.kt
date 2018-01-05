package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.Metadata
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class GetBrokerMetadataUseCase(
        private val brokerRepo: BrokerRepo
) : UseCase<BrokerCredentials, Metadata> {

    override
    fun execute(params: BrokerCredentials): Observable<Metadata> {
        return brokerRepo.openConnection(params).flatMap { it.metadata }
    }
}