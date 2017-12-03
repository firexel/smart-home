package com.seraph.smarthome.model

/**
 * Created by aleksandr.naumov on 11.11.2017.
 */

data class Network(
        val blocks: List<Block>,
        val connections: List<Connection>)

data class Block(
        val id: Block.Id,
        val name: String,
        val inputs: List<Endpoint>,
        val outputs: List<Endpoint>) {

    data class Id(val hash: String) {
        companion object {
            fun any(): Id = Id("+")
        }
    }
}

data class Endpoint(
        val id: Endpoint.Id,
        val name: String,
        val buffered: Boolean,
        val type: Endpoint.Type) {

    data class Id(val hash: String) {
        companion object {
            fun any(): Id = Id("+")
        }
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
        val block: Block.Id,
        val endpoint: Endpoint.Id)

data class Connection(
        val from: EndpointPath,
        val to: EndpointPath)

data class ConnectionsList(
        val list: List<Connection>
)
