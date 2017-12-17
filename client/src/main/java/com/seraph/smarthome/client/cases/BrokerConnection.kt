package com.seraph.smarthome.client.cases

import com.seraph.smarthome.model.Device
import com.seraph.smarthome.model.Metadata
import io.reactivex.Observable

interface BrokerConnection {
    fun observeDevices(): Observable<Collection<Device>>
    fun observeMetadata(): Observable<Metadata>
}