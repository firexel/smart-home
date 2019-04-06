package com.seraph.smarthome.io

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.Reader
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 10.03.18.
 */
data class ConfigNode(
        val buses: Map<String, SerialBusNode>
)

data class SerialBusNode(
        val settings: PortSettingsNode,
        val devices: Map<String, DeviceNode>
)

data class PortSettingsNode(
        val path: String,

        @SerializedName("baud_rate")
        val baudRate: Int,

        val parity: ParityNode,

        @SerializedName("data_bits")
        val dataBits: Int,

        @SerializedName("stop_bits")
        val stopBits: Int
)

enum class ParityNode {
    NO, ODD, EVEN, MARK, SPACE
}

data class DeviceNode(
        val driver: String,
        val settings: Any
)

private class ConfigDeserializer : JsonDeserializer<ConfigNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): ConfigNode {
        if (json is JsonObject) {
            val buses = context.deserialize<Map<String, SerialBusNode>>(
                    json.get("buses"),
                    object : TypeToken<Map<String, SerialBusNode>>() {}.type
            )
            return ConfigNode(buses)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

private class BusDeserializer : JsonDeserializer<SerialBusNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): SerialBusNode {
        if (json is JsonObject) {
            val portSettings = context.deserialize<PortSettingsNode>(json.get("settings"), PortSettingsNode::class.java)
            val devices = context.deserialize<Map<String, DeviceNode>>(
                    json.get("devices"),
                    object : TypeToken<Map<String, DeviceNode>>() {}.type
            )
            return SerialBusNode(portSettings, devices)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

private class DeviceNodeDeserializer(private val driverCatalogue: (String) -> DriverInfo) : JsonDeserializer<DeviceNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): DeviceNode {
        if (json is JsonObject) {
            val driverName = json.get("driver").asString
            val factory = driverCatalogue(driverName)
            val settings = context.deserialize<Any>(json.get("settings"), factory.settingsClass.java)
            return DeviceNode(driverName, settings)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

fun readConfig(reader: Reader, driverCatalogue: (String) -> DriverInfo): ConfigNode {
    val builder = GsonBuilder()
    builder.registerTypeAdapter(DeviceNode::class.java, DeviceNodeDeserializer(driverCatalogue))
    builder.registerTypeAdapter(SerialBusNode::class.java, BusDeserializer())
    builder.registerTypeAdapter(ConfigNode::class.java, ConfigDeserializer())
    return builder.create().fromJson(reader, ConfigNode::class.java)
}

data class DriverInfo(
        val settingsClass: KClass<*>
)