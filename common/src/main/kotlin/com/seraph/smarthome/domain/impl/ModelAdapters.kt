package com.seraph.smarthome.domain.impl

import com.google.gson.*
import com.google.gson.JsonSerializer
import com.seraph.smarthome.domain.*
import java.lang.reflect.Type

fun installModelAdapters(builder: GsonBuilder) {
    builder.registerTypeAdapter(Device.Id::class.java, DeviceIdAdapter())
    builder.registerTypeAdapter(Control.Id::class.java, ControlIdAdapter())
    builder.registerTypeAdapter(Endpoint.Id::class.java, EndpointIdAdapter())

    builder.registerTypeAdapter(Device::class.java, DeviceAdapter())
    builder.registerTypeAdapter(NormalizedUsage::class.java, NormalizedUsage.Deserializer())
}

private class DeviceIdAdapter : JsonSerializer<Device.Id>, JsonDeserializer<Device.Id> {
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

private class ControlIdAdapter : JsonSerializer<Control.Id>, JsonDeserializer<Control.Id> {
    override fun serialize(src: Control.Id, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.value)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Control.Id {
        if (json is JsonPrimitive && json.isString) {
            return Control.Id(json.asString)
        } else {
            throw JsonParseException("Cannot read Control.Id from $json")
        }
    }
}

private class EndpointIdAdapter : JsonSerializer<Endpoint.Id>, JsonDeserializer<Endpoint.Id> {
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
        val endpoints: List<NormalizedEndpoint>,
        val controls: List<NormalizedControl>
) {
    companion object {
        fun makeFromDevice(device: Device) = NormalizedDevice(
                id = device.id,
                endpoints = device.endpoints.map { NormalizedEndpoint.makeFromEndpoint(it) },
                controls = device.controls.map { NormalizedControl.makeFromControl(it) }
        )
    }

    fun makeDevice(): Device {
        val endpoints = endpoints.map { it.makeEndpoint() }
        val controls = controls.map { it.makeControl(endpoints) }
        return Device(id, endpoints, controls)
    }
}

private data class NormalizedEndpoint(
        val id: Endpoint.Id,
        val type: NormalizedType,
        val direction: Endpoint.Direction,
        val retention: Endpoint.Retention,
        val units: Units
) {
    companion object {
        fun makeFromEndpoint(endpoint: Endpoint<*>) = NormalizedEndpoint(
                id = endpoint.id,
                type = endpoint.type.accept(TypeEnumVisitor()),
                direction = endpoint.direction,
                retention = endpoint.retention,
                units = endpoint.units
        )
    }

    class TypeEnumVisitor : Endpoint.Type.Visitor<NormalizedType> {
        override fun onVoid(type: Endpoint.Type<Unit>): NormalizedType = NormalizedType.VOID
        override fun onBoolean(type: Endpoint.Type<Boolean>): NormalizedType = NormalizedType.BOOLEAN
        override fun onFloat(type: Endpoint.Type<Float>): NormalizedType = NormalizedType.FLOAT
    }

    fun makeEndpoint(): Endpoint<*> = Endpoint(
            id = id,
            direction = direction,
            retention = retention,
            units = units,
            type = when (type) {
                NormalizedType.BOOLEAN -> Types.BOOLEAN
                NormalizedType.VOID -> Types.VOID
                NormalizedType.FLOAT -> Types.FLOAT
            }
    )

    private enum class NormalizedType {
        BOOLEAN, VOID, FLOAT
    }
}

private data class NormalizedControl(
        val id: Control.Id,
        val priority: Control.Priority,
        val usage: NormalizedUsage
) {
    companion object {
        fun makeFromControl(control: Control) = NormalizedControl(
                id = control.id,
                priority = control.priority,
                usage = NormalizedUsage.makeFromUsage(control.usage)
        )
    }

    fun makeControl(endpoints: List<Endpoint<*>>): Control = Control(
            id = id,
            priority = priority,
            usage = usage.makeUsage(endpoints)
    )
}

private data class NormalizedUsage(
        val type: UsageType,
        val payload: Any
) {
    companion object {
        fun makeFromUsage(usage: Control.Usage) = usage.accept(SerializeVisitor())
    }

    fun makeUsage(endpoints: List<Endpoint<*>>): Control.Usage = when (type) {
        UsageType.BUTTON -> with(payload as ButtonPayload) {
            Button(endpoints.find(trigger, Types.VOID), alert)
        }
        UsageType.INDICATOR -> with(payload as IndicatorPayload) {
            Indicator(endpoints.find(source, Types.BOOLEAN))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> List<Endpoint<*>>.find(id: Endpoint.Id, type: Endpoint.Type<T>): Endpoint<T> {
        val endpoint = find { it.id == id && it.type == type } as Endpoint<T>?
        return endpoint ?: throw JsonParseException("Cannot find $id with appropriate type in $this")
    }

    class SerializeVisitor : Control.Usage.Visitor<NormalizedUsage> {
        override fun onButton(trigger: Endpoint<Unit>, alert: String): NormalizedUsage
                = NormalizedUsage(UsageType.BUTTON, ButtonPayload(trigger.id, alert))

        override fun onIndicator(source: Endpoint<Boolean>): NormalizedUsage
                = NormalizedUsage(UsageType.INDICATOR, IndicatorPayload(source.id))
    }

    private data class IndicatorPayload(val source: Endpoint.Id)

    private data class ButtonPayload(val trigger: Endpoint.Id, val alert: String)

    private enum class UsageType {
        BUTTON, INDICATOR
    }

    class Deserializer : JsonDeserializer<NormalizedUsage> {
        override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): NormalizedUsage {
            if (json is JsonObject) {
                val type = context.deserialize<UsageType>(json.get("type"), UsageType::class.java)
                val deserializeClass = when (type!!) {
                    UsageType.BUTTON -> ButtonPayload::class.java
                    UsageType.INDICATOR -> IndicatorPayload::class.java
                }
                val payload: Any = context.deserialize(json.get("payload"), deserializeClass)
                return NormalizedUsage(type, payload)
            } else {
                throw  JsonParseException("Unknown type of node $json")
            }
        }
    }
}