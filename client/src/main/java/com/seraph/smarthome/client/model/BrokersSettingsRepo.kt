package com.seraph.smarthome.client.model

import io.reactivex.Observable


/**
 * Created by alex on 09.12.17.
 */
public interface BrokersSettingsRepo {
    fun getBrokersSettings(): Observable<List<BrokerSettings>>
    fun saveBrokerSettings(brokerSettings: BrokerSettings): Observable<Unit>
    fun findBrokerSettings(id: Int): Observable<BrokerSettings?>
}

