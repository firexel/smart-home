package com.seraph.smarthome.logic

import com.google.gson.*
import java.io.Reader
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 10.03.18.
 */
data class ConfigNode(
        val devices: Map<String, DeviceNode>
)

data class DeviceNode(
        val driver: String,
        val settings: Any
)

private class DeviceNodeDeserializer(private val driverCatalogue: (String) -> DriverInfo) : JsonDeserializer<DeviceNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): DeviceNode {
        if (json is JsonObject) {
            val driverName = json.get("driver").asString
            val factory = driverCatalogue(driverName)
            val settings = if (factory.settingsClass != null) {
                context.deserialize<Any>(json.get("settings"), factory.settingsClass.java)
            } else {
                Any()
            }
            return DeviceNode(driverName, settings)
        } else {
            throw JsonParseException("Unknown type of node $json")
        }
    }
}

fun readConfig(reader: Reader, driverCatalogue: (String) -> DriverInfo): ConfigNode {
    val builder = GsonBuilder()
    builder.registerTypeAdapter(DeviceNode::class.java, DeviceNodeDeserializer(driverCatalogue))
    return builder.create().fromJson(reader, ConfigNode::class.java)
}

data class DriverInfo(
        val settingsClass: KClass<*>?
)