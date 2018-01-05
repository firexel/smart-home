package com.seraph.smarthome.client.model

import android.os.Build
import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.client.cases.BrokerRepo
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
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
                val broker = StatefulMqttBroker(
                        "tcp://${credentials.host}:${credentials.port}",
                        "Mobile client " + Build.DEVICE,
                        log.copy("Broker")
                )
                BrokerConnectionImpl(broker) as BrokerConnection
            }.cache()
        }.subscribeOn(Schedulers.io())
    }
}