package com.seraph.smarthome.domain

import com.seraph.smarthome.domain.impl.EndpointAddrSerializer
import kotlinx.serialization.Serializable

@Serializable(with = EndpointAddrSerializer::class)
data class EndpointAddr(
        val device: Device.Id,
        val endpoint: Endpoint.Id,
)

@Serializable
data class WidgetGroup(
        val name: String,
        val widgets: List<Widget>,
)

@Serializable
data class Widget(
        val name: String,
        val category: Category,
        val state: StateTrait? = null,
        val target: TargetTrait? = null,
        val toggle: ToggleTrait? = null,
        // val actions: ActionsTrait? = null // not implemented yet
) {

    @Serializable
    enum class Category {
        GAUGE, THERMOSTAT, LIGHT, SWITCH
    }

    @Serializable
    sealed class StateTrait {
        @Serializable
        data class Binary(val endpoint: EndpointAddr) : StateTrait()

        @Serializable
        data class Numeric(val endpoint: EndpointAddr, val precision: Int = 0) : StateTrait()
    }

    @Serializable
    sealed class TargetTrait {
        @Serializable
        data class Binary(val endpoint: EndpointAddr) : TargetTrait()

        @Serializable
        data class Numeric(
                val endpoint: EndpointAddr,
                val min: Float,
                val max: Float,
        ) : TargetTrait()
    }

    @Serializable
    sealed class ToggleTrait {
        @Serializable
        data class Action(val endpoint: EndpointAddr) : ToggleTrait()

        @Serializable
        data class Invert(
                val endpointRead: EndpointAddr,
                val endpointWrite: EndpointAddr,
        ) : ToggleTrait()

        @Serializable
        data class OnOffActions(
                val endpointRead: EndpointAddr,
                val endpointOn: EndpointAddr,
                val endpointOff: EndpointAddr,
        ) : ToggleTrait()
    }
}