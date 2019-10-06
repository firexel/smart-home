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
        val rs485Buses: Map<String, Rs485BusNode>,
        val dmx512: DmxNode?
)

data class DmxNode(
        val settings: DmxGlobalSettingsNode,
        val universes: Map<String, DmxUniverseNode>
)

data class DmxGlobalSettingsNode(
        @SerializedName("olad_host")
        val oladHost: String,

        @SerializedName("olad_port")
        val oladPort: Int
)

data class DmxUniverseNode(
        @SerializedName("device_name")
        val deviceName: String,

        @SerializedName("device_port")
        val devicePort: Int,

        val fixtures: Map<String, DmxFixtureNode>
)

data class DmxFixtureNode(
        @SerializedName("address_at_bus")
        val addressAtBus: Int,

        val driver: String
)

data class Rs485BusNode(
        val settings: Rs485PortSettingsNode,
        val devices: Map<String, Rs485DeviceNode>
)

data class Rs485PortSettingsNode(
        val path: String,

        @SerializedName("baud_rate")
        val baudRate: Int,

        val parity: Rs485ParityNode,

        @SerializedName("data_bits")
        val dataBits: Int,

        @SerializedName("stop_bits")
        val stopBits: Int
)

enum class Rs485ParityNode {
    NO, ODD, EVEN, MARK, SPACE
}

data class Rs485DeviceNode(
        val driver: String,
        val settings: Any
)

private class ConfigDeserializer : JsonDeserializer<ConfigNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): ConfigNode {
        if (json is JsonObject) {
            val rs485Buses = context.deserialize<Map<String, Rs485BusNode>>(
                    json.get("rs485"),
                    object : TypeToken<Map<String, Rs485BusNode>>() {}.type
            )
            val dmxNode = if (json.has("dmx512")) {
                context.deserialize<DmxNode>(json.get("dmx512"), DmxNode::class.java)
            } else {
                null
            }
            return ConfigNode(rs485Buses, dmxNode)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

private class DmxNodeDeserializer : JsonDeserializer<DmxNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): DmxNode {
        if (json is JsonObject) {
            val universeMap = context.deserialize<Map<String, DmxUniverseNode>>(
                    json.get("universes"),
                    object : TypeToken<Map<String, DmxUniverseNode>>() {}.type
            )
            val settings = context.deserialize<DmxGlobalSettingsNode>(json.get("settings"), DmxGlobalSettingsNode::class.java)
            return DmxNode(settings, universeMap)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

private class DmxUniverseNodeDesirializer : JsonDeserializer<DmxUniverseNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): DmxUniverseNode {
        if (json is JsonObject) {
            val fixtures = context.deserialize<Map<String, DmxFixtureNode>>(
                    json.get("fixtures"),
                    object : TypeToken<Map<String, DmxFixtureNode>>() {}.type
            )
            val deviceName = json.get("device_name").asString
            val devicePort = json.get("device_port").asInt
            return DmxUniverseNode(deviceName, devicePort, fixtures)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

private class BusDeserializer : JsonDeserializer<Rs485BusNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): Rs485BusNode {
        if (json is JsonObject) {
            val portSettings = context.deserialize<Rs485PortSettingsNode>(json.get("settings"), Rs485PortSettingsNode::class.java)
            val devices = context.deserialize<Map<String, Rs485DeviceNode>>(
                    json.get("devices"),
                    object : TypeToken<Map<String, Rs485DeviceNode>>() {}.type
            )
            return Rs485BusNode(portSettings, devices)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

private class DeviceNodeDeserializer(private val driverCatalogue: (String) -> DriverInfo) : JsonDeserializer<Rs485DeviceNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): Rs485DeviceNode {
        if (json is JsonObject) {
            val driverName = json.get("driver").asString
            val factory = driverCatalogue(driverName)
            val settings = context.deserialize<Any>(json.get("settings"), factory.settingsClass.java)
            return Rs485DeviceNode(driverName, settings)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

fun readConfig(reader: Reader, driverCatalogue: (String) -> DriverInfo): ConfigNode {
    val builder = GsonBuilder()
    builder.registerTypeAdapter(Rs485DeviceNode::class.java, DeviceNodeDeserializer(driverCatalogue))
    builder.registerTypeAdapter(Rs485BusNode::class.java, BusDeserializer())
    builder.registerTypeAdapter(DmxNode::class.java, DmxNodeDeserializer())
    builder.registerTypeAdapter(DmxUniverseNode::class.java, DmxUniverseNodeDesirializer())
    builder.registerTypeAdapter(ConfigNode::class.java, ConfigDeserializer())
    return builder.create().fromJson(reader, ConfigNode::class.java)
}

data class DriverInfo(
        val settingsClass: KClass<*>
)