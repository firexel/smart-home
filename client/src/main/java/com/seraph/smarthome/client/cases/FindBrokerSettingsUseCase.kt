package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

class FindBrokerSettingsUseCase(private val repo: BrokersSettingsRepo) : UseCase<Int, BrokerSettings?> {
    override fun execute(params: Int): Observable<BrokerSettings?> = repo.findBrokerSettings(params)
}