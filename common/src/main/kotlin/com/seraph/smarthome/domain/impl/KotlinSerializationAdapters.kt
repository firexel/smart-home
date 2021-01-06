package com.seraph.smarthome.domain.impl

import com.seraph.smarthome.domain.*
import com.seraph.smarthome.domain.BaseStringConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json


object EndpointAddrSerializer : KSerializer<EndpointAddr> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "com.seraph.smarthome.domain.impl.EndpointAddrSerializer", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: EndpointAddr) {
        encoder.encodeString("${value.device}/${value.endpoint}")
    }

    override fun deserialize(decoder: Decoder): EndpointAddr {
        val string = decoder.decodeString()
        val parts = string.split("/")
        require(parts.size == 2) { "$string has wrong format" }
        return EndpointAddr(
                Device.Id(parts[0].split(":")),
                Endpoint.Id(parts[1])
        )
    }
}

internal class KotlinMetainfoSerializer : BaseStringConverter<Metainfo>() {

    override fun fromString(string: String): Metainfo {
        return Json.decodeFromString(string)
    }

    override fun toString(data: Metainfo): String = Json.encodeToString(data)
}