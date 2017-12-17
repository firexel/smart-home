package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerInfo
import com.seraph.smarthome.client.model.BrokersInfoRepo
import com.seraph.smarthome.client.presentation.UseCase
import io.reactivex.Observable

class ListBrokerInfoUseCase(private val repo: BrokersInfoRepo) : UseCase<Unit, List<BrokerInfo>> {
    override fun execute(params: Unit): Observable<List<BrokerInfo>> = repo.getBrokersSettings()
}