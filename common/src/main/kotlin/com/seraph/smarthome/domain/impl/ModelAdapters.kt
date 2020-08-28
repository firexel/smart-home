package com.seraph.smarthome.domain.impl

import com.google.gson.*
import com.google.gson.JsonSerializer
import com.seraph.smarthome.domain.*
import java.lang.reflect.Type

fun installModelAdapters(builder: GsonBuilder) {
    builder.registerTypeAdapter(Device.Id::class.java, DeviceIdAdapter())
    builder.registerTypeAdapter(Endpoint.Id::class.java, EndpointIdAdapter())

    builder.registerTypeAdapter(Device::class.java, DeviceAdapter())
}

class DeviceIdAdapter : JsonSerializer<Device.Id>, JsonDeserializer<Device.Id> {
    override fun serialize(src: Device.Id, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.segments.joinToString(":"))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Device.Id {
        if (json is JsonPrimitive && json.isString) {
            return Device.Id(json.asString.split(':'))
        } else {
            throw JsonParseException("Cannot read Device.Id from $json")
        }
    }
}

class EndpointIdAdapter : JsonSerializer<Endpoint.Id>, JsonDeserializer<Endpoint.Id> {
    override fun serialize(src: Endpoint.Id, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.value)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Endpoint.Id {
        if (json is JsonPrimitive && json.isString) {
            return Endpoint.Id(json.asString)
        } else {
            throw JsonParseException("Cannot read Endpoint.Id from $json")
        }
    }
}

private class DeviceAdapter : JsonSerializer<Device>, JsonDeserializer<Device> {
    override fun serialize(src: Device, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        return context.serialize(NormalizedDevice.makeFromDevice(src))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): Device {
        return context.deserialize<NormalizedDevice>(json, NormalizedDevice::class.java).makeDevice()
    }
}

private data class NormalizedDevice(
        val id: Device.Id,
        val endpoints: List<NormalizedEndpoint>
) {
    companion object {
        fun makeFromDevice(device: Device) = NormalizedDevice(
                id = device.id,
                endpoints = device.endpoints.map { NormalizedEndpoint.makeFromEndpoint(it) }
        )
    }

    fun makeDevice(): Device {
        val endpoints = endpoints.map { it.makeEndpoint() }
        return Device(id, endpoints)
    }
}

private data class NormalizedEndpoint(
        val id: Endpoint.Id,
        val type: NormalizedType,
        val direction: Endpoint.Direction,
        val retention: Endpoint.Retention,
        val dataKind: Endpoint.DataKind,
        val interaction: Endpoint.Interaction,
        val units: Units
) {
    companion object {
        fun makeFromEndpoint(endpoint: Endpoint<*>) = NormalizedEndpoint(
                id = endpoint.id,
                type = endpoint.type.accept(TypeEnumVisitor()),
                direction = endpoint.direction,
                retention = endpoint.retention,
                units = endpoint.units,
                dataKind = endpoint.dataKind,
                interaction = endpoint.interaction
        )
    }

    class TypeEnumVisitor : Endpoint.Type.Visitor<NormalizedType> {
        override fun onInt(type: Endpoint.Type<Int>): NormalizedType = NormalizedType.INTEGER
        override fun onVoid(type: Endpoint.Type<Unit>): NormalizedType = NormalizedType.VOID
        override fun onBoolean(type: Endpoint.Type<Boolean>): NormalizedType = NormalizedType.BOOLEAN
        override fun onFloat(type: Endpoint.Type<Float>): NormalizedType = NormalizedType.FLOAT
        override fun onDeviceState(type: Endpoint.Type<DeviceState>): NormalizedType = NormalizedType.DEVICE_STATE
    }

    fun makeEndpoint(): Endpoint<*> = Endpoint(
            id = id,
            direction = direction,
            retention = retention,
            units = units,
            dataKind = dataKind,
            interaction = interaction,
            type = when (type) {
                NormalizedType.BOOLEAN -> Types.BOOLEAN
                NormalizedType.VOID -> Types.VOID
                NormalizedType.FLOAT -> Types.FLOAT
                NormalizedType.DEVICE_STATE -> Types.DEVICE_STATE
                NormalizedType.INTEGER -> Types.INTEGER
            }
    )

    private enum class NormalizedType {
        BOOLEAN, VOID, FLOAT, INTEGER, DEVICE_STATE
    }
}