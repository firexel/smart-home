package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class AddBrokerSettingsUseCase(
        private val settingsRepo: BrokersSettingsRepo,
        private val brokerRepo: BrokerRepo) : UseCase<BrokerSettings, Unit> {

    override
    fun execute(params: BrokerSettings): Observable<Unit> {
        return brokerRepo.openConnection(params)
                .flatMap { settingsRepo.saveBrokerSettings(params) }
    }
}

