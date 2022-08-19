package com.seraph.smarthome.wirenboard

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.net.URI
import java.net.URISyntaxException

object AddressAsStringSerializer : KSerializer<Address> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.seraph.smarthome.wirenboard.Address", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Address) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Address {
        val uri = URI(decoder.decodeString())
        if (uri.host == null || uri.port == -1) {
            throw URISyntaxException(
                uri.toString(),
                "URI must have host and port parts"
            )
        }
        return Address(uri.scheme, uri.host, uri.port)
    }
}

@Serializable(with = AddressAsStringSerializer::class)
data class Address(val scheme: String, val host: String, val port: Int) {
    override fun toString(): String {
        return "${scheme}://${host}:${port}"
    }
}

@Serializable
data class Credentials(
    val login: String,
    val passwd: String
)

@Serializable
data class Network(
    val name: String,
    val address: Address,
    val credentials: Credentials? = null,
)

@Serializable
data class Excludes(
    val devices: List<String>,
    val endpoints: List<String>
)

@Serializable
data class DeviceRename(
    val id: String,
    val name: String,
    val endpoints: List<EndpointRename>?
)

@Serializable
data class EndpointRename(
    val id: String,
    val name: String
)

@Serializable
data class Config(
    val wirenboard: Network,
    val smarthome: Network,
    val exclude: Excludes,
    val rename: List<DeviceRename>
)

@OptIn(ExperimentalSerializationApi::class)
fun readConfig(file: File): Config {
    return Json.decodeFromString(FileReader(file).readText())
}