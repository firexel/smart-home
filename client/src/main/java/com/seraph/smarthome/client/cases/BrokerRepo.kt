package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.BrokerCredentials
import io.reactivex.Observable

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
interface BrokerRepo {
    fun openConnection(credentials: BrokerCredentials): Observable<BrokerConnection>
}

