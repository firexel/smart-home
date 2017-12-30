package com.seraph.smarthome.client.model

import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 30.12.17.
 */

typealias CommonDevice = com.seraph.smarthome.model.Device
typealias CommonDeviceId = com.seraph.smarthome.model.Device.Id
typealias CommonMetadata = com.seraph.smarthome.model.Metadata
typealias CommonProperty = com.seraph.smarthome.model.Property
typealias CommonPurpose = com.seraph.smarthome.model.Property.Purpose
typealias CommonPropertyType = com.seraph.smarthome.model.Property.Type
typealias CommonEndpointId = com.seraph.smarthome.model.Endpoint.Id

fun Device.Id.map(): CommonDeviceId = CommonDeviceId(hash)

fun Property.Id.map(): CommonEndpointId = CommonEndpointId(hash)

fun CommonMetadata.map(): Metadata = Metadata(this.name)

fun CommonDevice.map(storage: PropertyStorage): Device {
    val newId = Device.Id(id.hash)
    return Device(newId, name, properties.map { it.map(newId, storage) })
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

interface PropertyStorage {
    fun <T : Any> getValueFor(deviceId: Device.Id, propertyId: Property.Id, clazz: KClass<T>): T
}
