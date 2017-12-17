package com.seraph.smarthome.client.model

import io.reactivex.Observable


/**
 * Created by alex on 09.12.17.
 */
public interface BrokersInfoRepo {
    fun getBrokersSettings(): Observable<List<BrokerInfo>>
    fun saveBrokerSettings(brokerSettings: BrokerInfo): Observable<Unit>
}

