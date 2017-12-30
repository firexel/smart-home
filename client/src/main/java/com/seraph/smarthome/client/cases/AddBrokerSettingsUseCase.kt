package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import com.seraph.smarthome.client.model.BrokerInfo
import com.seraph.smarthome.client.model.BrokersInfoRepo
import com.seraph.smarthome.client.model.Metadata
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class AddBrokerSettingsUseCase(
        private val infoRepo: BrokersInfoRepo,
        private val brokerRepo: BrokerRepo) : UseCase<BrokerCredentials, Unit> {

    override
    fun execute(params: BrokerCredentials): Observable<Unit> {
        return brokerRepo.openConnection(params)
                .flatMap {
                    it.metadata
                            .take(1)
                            .timeout(2, TimeUnit.SECONDS)
                            .onErrorReturn {
                                if (it is TimeoutException) Metadata("Unknown broker")
                                else throw it
                            }
                }
                .flatMap { infoRepo.saveBrokerSettings(BrokerInfo(it, params)) }
    }
}

