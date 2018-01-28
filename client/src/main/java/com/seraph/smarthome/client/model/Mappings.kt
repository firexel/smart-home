package com.seraph.smarthome.client.model

import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Control
import com.seraph.smarthome.transport.Endpoint
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 30.12.17.
 */

typealias CommonDevice = com.seraph.smarthome.transport.Device
typealias CommonDeviceId = com.seraph.smarthome.transport.Device.Id
typealias CommonMetadata = com.seraph.smarthome.transport.Metadata
typealias CommonProperty = Control
typealias CommonPurpose = Control.Purpose
typealias CommonPropertyType = Control.Type
typealias CommonEndpointId = Endpoint.Id

fun Device.Id.map(): CommonDeviceId = CommonDeviceId(hash)

fun Property.Id.map(): CommonEndpointId = CommonEndpointId(hash)

fun CommonMetadata.map(): Metadata = Metadata(this.name)

fun CommonDevice.map(storage: PropertyStorage): Device {
    val newId = Device.Id(id.hash)
    return Device(newId, name, controls.map { it.map(newId, storage) })
}

fun CommonPurpose.map(): Property.Priority = when (this) {
    CommonPurpose.MAIN -> Property.Priority.MAIN
    CommonPurpose.PRIMARY -> Property.Priority.PRIMARY
    CommonPurpose.SECONDARY -> Property.Priority.SECONDARY
}

fun CommonProperty.map(deviceId: Device.Id, storage: PropertyStorage): Property<*> {
    val newId = Property.Id(id.hash)
    val newName = id.hash
    val newPriority = purpose.map()
    return when (this.type) {
        CommonPropertyType.ACTION -> ActionProperty(newId, newName, newPriority)
        CommonPropertyType.INDICATOR -> IndicatorProperty(
                newId, newName, newPriority,
                storage.getValueFor(deviceId, newId, Boolean::class))
    }
}

fun Broker.BrokerState.map(): BrokerConnection.State = this.accept(MapVisitor())

class MapVisitor : Broker.BrokerState.Visitor<BrokerConnection.State> {
    override fun onConnectedState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onConnectedState()
        }
    }

    override fun onDisconnectedState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onDisconnectedState()
        }
    }

    override fun onDisconnectingState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onDisconnectingState()
        }
    }

    override fun onWaitingState(msToReconnect: Long): BrokerConnection.State = object : BrokerConnection.State {
        private val creationTime = System.currentTimeMillis()

        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onWaitingState(
                    (creationTime + msToReconnect) - System.currentTimeMillis()
            )
        }
    }

    override fun onConnectingState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onConnectingState()
        }
    }
}

interface PropertyStorage {
    fun <T : Any> getValueFor(deviceId: Device.Id, propertyId: Property.Id, clazz: KClass<T>): T
}
