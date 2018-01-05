package com.seraph.smarthome.client.cases

import com.seraph.smarthome.client.model.Device
import com.seraph.smarthome.client.model.Metadata
import com.seraph.smarthome.client.model.Property
import io.reactivex.Observable

interface BrokerConnection {
    /**
     * This observable fires when new device is appeared or disappeared, name, property change etc
     * It holds actual state of device list
     */
    val devices: Observable<List<Device>>

    /**
     * This observable fires when broker metadata has been loaded or changed
     * It holds actual state of broker metadata
     */
    val metadata: Observable<Metadata>

    /**
     * This observable fires when connection state changes
     * It holds actual state. Initially state is Connecting
     */
    val state: Observable<State>

    /**
     * This method sends property change signal to the broker
     */
    fun <T> change(deviceId: Device.Id, property: Property<T>, value: T): Observable<Unit>

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