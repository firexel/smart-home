package com.seraph.smarthome.connector

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.impl.installModelAdapters
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type

data class Config(
        val devices: Map<Device.Id, DeviceConfig>
) {
    fun mapToConnectionsList(): List<Connection> {
        val list = mutableListOf<Connection>()
        devices.entries.forEach {
            val srcDevice = it.key
            it.value.endpoints.forEach {
                val srcEndpoint = it.key
                it.value.destinations.forEach {
                    list.add(Connection(srcDevice, srcEndpoint, it.device, it.endpoint))
                }
            }
        }
        return list
    }
}

data class DeviceConfig(
        val endpoints: Map<Endpoint.Id, EndpointConfig>
)

data class EndpointConfig(
        val destinations: List<GlobalEndpointId>
)

data class Connection(
        val srcDevice: Device.Id,
        val srcEndpoint: Endpoint.Id,
        val dstDevice: Device.Id,
        val dstEndpoint: Endpoint.Id
)

data class GlobalEndpointId(
        val device: Device.Id,
        val endpoint: Endpoint.Id
) {
    override fun toString(): String = "$device::$endpoint"
}

fun readConfig(config: File): Config {
    val gsonBuilder = GsonBuilder()
    installModelAdapters(gsonBuilder)
    installConfigAdapters(gsonBuilder)
    val gson = gsonBuilder.create()
    return gson.fromJson(FileReader(config), Config::class.java)
}

fun installConfigAdapters(builder: GsonBuilder) {
    builder.registerTypeAdapter(GlobalEndpointId::class.java, GlobalEndpointIdAdapter())
    builder.registerTypeAdapter(EndpointConfig::class.java, EndpointConfigAdapter())
    builder.registerTypeAdapter(DeviceConfig::class.java, DeviceConfigAdapter())
    builder.registerTypeAdapter(Config::class.java, RootConfigAdapter())
}

private class GlobalEndpointIdAdapter : JsonDeserializer<GlobalEndpointId> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): GlobalEndpointId {
        if (json is JsonPrimitive && json.isString) {
            val segments = json.asString.split('/')
            if (segments.size != 2) {
                throw JsonParseException("Expecting exactly 2 segments it $json")
            }
            val deviceId = context.deserialize<Device.Id>(
                    JsonPrimitive(segments[0]), Device.Id::class.java
            )
            val endpointId = context.deserialize<Endpoint.Id>(
                    JsonPrimitive(segments[1]), Endpoint.Id::class.java
            )
            return GlobalEndpointId(deviceId, endpointId)
        } else {
            throw JsonParseException("Cannot read GlobalEndpointId from $json")
        }
    }
}

private class EndpointConfigAdapter : JsonDeserializer<EndpointConfig> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): EndpointConfig {
        return if (json is JsonPrimitive && json.isString) {
            EndpointConfig(listOf(parseGlobalEndpoint(context, json.asString)))
        } else if (json is JsonArray) {
            EndpointConfig(json.map { parseGlobalEndpoint(context, it.asString) })
        } else {
            throw JsonParseException("Cannot read EndpointConfig from $json")
        }
    }

    private fun parseGlobalEndpoint(context: JsonDeserializationContext, string: String?): GlobalEndpointId {
        return context.deserialize(JsonPrimitive(string), GlobalEndpointId::class.java)
    }
}

private class DeviceConfigAdapter : JsonDeserializer<DeviceConfig> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): DeviceConfig {
        if (json is JsonObject) {
            val map = context.deserialize<Map<Endpoint.Id, EndpointConfig>>(
                    json,
                    object : TypeToken<Map<Endpoint.Id, EndpointConfig>>() {}.type
            )
            return DeviceConfig(map)
        } else {
            throw JsonParseException("Cannot read DeviceConfig from $json")
        }
    }
}

private class RootConfigAdapter : JsonDeserializer<Config> {
    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): Config {
        if (json is JsonObject) {
            val map = context.deserialize<Map<Device.Id, DeviceConfig>>(
                    json,
                    object : TypeToken<Map<Device.Id, DeviceConfig>>() {}.type
            )
            return Config(map)
        } else {
            throw JsonParseException("Cannot read Config from $json")
        }
    }
}





