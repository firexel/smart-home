package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerSettings
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
interface BrokerRepo {
    fun openConnection(settings: BrokerSettings): Observable<BrokerConnection>
}

