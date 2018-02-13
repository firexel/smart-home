package com.seraph.smarthome.client.model

import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.client.cases.BrokerRepo
import com.seraph.smarthome.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class MqttBrokerRepo(private val log: Log) : BrokerRepo {

    private val openConnections = mutableMapOf<BrokerCredentials, Observable<BrokerConnection>>()

    override fun openConnection(credentials: BrokerCredentials): Observable<BrokerConnection> {
        return openConnections.getOrPut(credentials) {
            Observable.fromCallable {
                BrokerConnectionImpl(credentials, log.copy("Connection")) as BrokerConnection
            }.cache()
        }.subscribeOn(Schedulers.io())
    }
}