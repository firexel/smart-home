package com.seraph.smarthome.client.cases

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Metainfo
import io.reactivex.Observable

interface BrokerConnection {
    /**
     * This observable fires when new device is appeared or disappeared, name, property publish etc
     * It holds actual state of device list
     */
    val devices: Observable<List<Device>>

    /**
     * This observable fires when broker metainfo has been loaded or changed
     * It holds actual state of broker metainfo
     */
    val metainfo: Observable<Metainfo>

    /**
     * This observable fires when connection state changes
     * It holds actual state. Initially state is Connecting
     */
    val state: Observable<State>

    /**
     * This method sends endpoint publish signal to the broker
     */
    fun <T> publish(deviceId: Device.Id, endpoint: Endpoint<T>, value: T): Observable<Unit>

    /**
     * This method receives endpoint publishes from the entire system
     */
    fun <T> subscribe(deviceId: Device.Id, endpoint: Endpoint<T>): Observable<T>

    /**
     * Abstraction for connection state
     */
    interface State {
        fun <T> accept(visitor: Visitor<T>): T

        interface Visitor<out T> {
            fun onConnectedState(): T
            fun onDisconnectedState(): T
            fun onDisconnectingState(): T
            fun onWaitingState(msToReconnect: Long): T
            fun onConnectingState(): T
        }
    }
}