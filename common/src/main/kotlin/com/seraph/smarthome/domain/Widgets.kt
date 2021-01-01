package com.seraph.smarthome.domain

import com.seraph.smarthome.domain.impl.EndpointAddrSerializer
import kotlinx.serialization.Serializable

@Serializable(with = EndpointAddrSerializer::class)
data class EndpointAddr(
        val device: Device.Id,
        val endpoint: Endpoint.Id
)

@Serializable
data class WidgetGroup(
        val name: String,
        val widgets: List<Widget>
)

@Serializable
sealed class Widget {

    abstract val name: String

    @Serializable
    data class BinaryLight(
            override val name: String,
            val state: EndpointAddr,
            val onOff: EndpointAddr
    ) : Widget()

    @Serializable
    data class DimmableLight(
            override val name: String,
            val state: EndpointAddr,
            val onOff: EndpointAddr
    ) : Widget()

    @Serializable
    data class OnOffSwitch(
            override val name: String,
            val state: EndpointAddr,
            val onOff: EndpointAddr
    ) : Widget()

    @Serializable
    data class Thermostat(
            override val name: String,
            val state: EndpointAddr,
            val setPoint: EndpointAddr,
            val minTemp: Float,
            val maxTemp: Float
    ) : Widget()

    @Serializable
    data class Gauge(
            override val name: String,
            val state: EndpointAddr,
            val dangerBelow: Float,
            val dangerAbove: Float
    ) : Widget()
}