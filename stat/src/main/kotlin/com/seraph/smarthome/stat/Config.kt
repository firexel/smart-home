package com.seraph.smarthome.stat

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
            "com.seraph.smarthome.stat.Address", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Address) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Address {
        val uri = URI(decoder.decodeString())
        if (uri.host == null || uri.port == -1) {
            throw URISyntaxException(uri.toString(),
                    "URI must have host and port parts")
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
sealed class Output

@Serializable
data class GraphiteOutput(
        val address: Address,
        val repeatDelaySeconds: Int,
        val configFolderPath: String? = null
) : Output()

@Serializable
data class Config(
        val network: Address,
        val outputs: List<Output>
)

fun readConfig(file: File): Config {
    return Json.decodeFromString(FileReader(file).readText())
}