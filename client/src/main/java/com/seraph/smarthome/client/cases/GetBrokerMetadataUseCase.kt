package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.presentation.UseCase
import com.seraph.smarthome.domain.Metainfo
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class GetBrokerMetadataUseCase(
        private val brokerRepo: BrokerRepo
) : UseCase<BrokerCredentials, Metainfo> {

    override
    fun execute(params: BrokerCredentials): Observable<Metainfo> {
        return brokerRepo.openConnection(params).flatMap { it.metainfo }
    }
}