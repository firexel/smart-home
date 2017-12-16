package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerSettings
import com.seraph.smarthome.client.model.BrokersSettingsRepo
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

class ListBrokerSettingsUseCase(private val repo: BrokersSettingsRepo) : UseCase<Unit, List<BrokerSettings>> {
    override fun execute(params: Unit): Observable<List<BrokerSettings>> = repo.getBrokersSettings()
}