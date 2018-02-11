package com.seraph.smarthome.client.model

import android.os.Build
import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.impl.StatefulMqttBroker
import com.seraph.smarthome.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject


class BrokerConnectionImpl(credentials: BrokerCredentials, log: Log) : BrokerConnection {

    private val deviceMap = mutableMapOf<Device.Id, Device>()
    private val devicesSubject = newReplaySubject<List<Device>>()
    private val metadataSubject = newReplaySubject<Metainfo>()
    private val stateSubject = newReplaySubject<BrokerConnection.State>()
    private val network: Network

    override val devices: Observable<List<Device>> = devicesSubject
    override val metainfo: Observable<Metainfo> = metadataSubject
    override val state: Observable<BrokerConnection.State> = stateSubject

    init {
        val broker = StatefulMqttBroker(
                "tcp://${credentials.host}:${credentials.port}",
                "Mobile client " + Build.DEVICE,
                log.copy("Broker")
        )
        broker.addStateListener(object : Broker.StateListener {
            override fun onStateChanged(brokerState: Broker.BrokerState) {
                handleStateChange(brokerState)
            }
        })
        network = MqttNetwork(broker, log.copy("Network"))
        network.subscribe {
            metadataSubject.onNext(it)
        }
        network.subscribe(null) {
            handleDevicePublished(it)
        }
    }

    private fun <T> newReplaySubject() = ReplaySubject.createWithSize<T>(1)
            .apply { subscribeOn(Schedulers.io()) }

    private fun handleStateChange(brokerState: Broker.BrokerState) {
        stateSubject.onNext(brokerState.map())
    }

    private fun handleDevicePublished(device: Device) {
        deviceMap[device.id] = device
        devicesSubject.onNext(ArrayList(deviceMap.values))
    }

    override fun <T> publish(deviceId: Device.Id, endpoint: Endpoint<T>, value: T): Observable<Unit> {
        return Observable.fromCallable {
            network.publish(deviceId, endpoint, value)
        }
    }

    override fun <T> subscribe(deviceId: Device.Id, endpoint: Endpoint<T>): Observable<T> {
        return newReplaySubject<T>().apply {
            network.subscribe(deviceId, endpoint) { _, _, data ->
                onNext(data)
            }
        }
    }
}