package com.seraph.smarthome.client.model

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.client.cases.BrokerRepo
import com.seraph.smarthome.model.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import java.util.*

/**
 * Created by aleksandr.naumov on 16.12.17.
 */
class MqttBrokerRepo(private val log: Log) : BrokerRepo {

    override fun openConnection(credentials: BrokerCredentials): Observable<BrokerConnection> {
        return Observable.fromCallable {
            val broker = MqttBroker(
                    "tcp://${credentials.host}:${credentials.port}",
                    "Mobile client",
                    log
            )
            MqttBrokerConnection(log, broker) as BrokerConnection
        }.subscribeOn(Schedulers.io())
    }

    class MqttBrokerConnection(private val log: Log, private val broker: MqttBroker) : BrokerConnection {
        override fun observeDevices(): Observable<Collection<Device>> {
            val subject = ReplaySubject.createWithSize<Collection<Device>>(1)
            val setOfDevices = TreeSet<Device>(compareBy { it.id })
            broker.subscribe(Topics.structure(Device.Id.any())) { topic, data ->
                try {
                    val device = Gson().fromJson(data, Device::class.java)
                    setOfDevices.add(device)
                    subject.onNext(setOfDevices)
                } catch (ex: JsonSyntaxException) {
                    log.w("Got message with bad syntax from $topic")
                }
            }
            return subject
        }

        override fun observeMetadata(): Observable<Metadata> {
            val subject = ReplaySubject.createWithSize<Metadata>(1)
            broker.subscribe(Topics.metadata()) { topic, data ->
                try {
                    val metadata = Gson().fromJson(data, Metadata::class.java)
                    subject.onNext(metadata)
                } catch (ex: JsonSyntaxException) {
                    log.w("Got message with bad syntax from $topic")
                }
            }
            return subject
        }
    }
}