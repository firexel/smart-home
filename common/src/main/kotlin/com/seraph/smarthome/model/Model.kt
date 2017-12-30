package com.seraph.smarthome.model

/**
 * Created by aleksandr.naumov on 11.11.2017.
 */

data class Network(
        val devices: List<Device>,
        val connections: List<Connection>)

data class Device(
        val id: Device.Id,
        val name: String,
        val inputs: List<Endpoint> = emptyList(),
        val outputs: List<Endpoint> = emptyList(),
        val properties: List<Property> = emptyList()) {

    data class Id(val hash: String) : Comparable<Id> {
        companion object {
            fun any(): Id = Id("+")
        }

        override fun compareTo(other: Id): Int {
            return hash.compareTo(other.hash)
        }

        override fun toString() = hash
    }
}

data class Endpoint(
        val id: Endpoint.Id,
        val name: String,
        val type: Endpoint.Type) {

    data class Id(val hash: String) {
        companion object {
            fun any(): Id = Id("+")
        }

        override fun toString() = hash
    }

    enum class Type {
        INTEGER,
        STRING,
        BOOLEAN,
        DATE,
        PERCENT,
        FLOAT
    }
}

data class EndpointPath(
        val device: Device.Id,
        val endpoint: Endpoint.Id)

data class Connection(
        val from: EndpointPath,
        val to: EndpointPath)

data class ConnectionsList(
        val list: List<Connection>
)

data class Metadata(
        val name: String
)

data class Property(
        val id:Endpoint.Id,
        val type: Type,
        val purpose: Purpose
) {

    enum class Type {
        ACTION, // button-type property
        INDICATOR // true/false indicator property
    }

    enum class Purpose {
        MAIN, // control with this purpose will be interpreted as main control of whole device (main switch, functioning mode, etc)
        PRIMARY, // controls wit this purpose should be accessible in view mode
        SECONDARY // controls with this purpose can be accessible in edit mode
    }
}
