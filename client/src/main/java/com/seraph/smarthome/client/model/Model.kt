package com.seraph.smarthome.client.model

import java.io.Serializable

/**
 * Created by aleksandr.naumov on 29.12.17.
 */

data class Metadata(val brokerName: String)

data class Device(val id: Id, val name: String, val properties: List<Property<*>>) : Comparable<Device> {
    override fun compareTo(other: Device): Int = id.hash.compareTo(other.id.hash)

    data class Id(val hash: String)
}

abstract class Property<V>(val id: Id, val name: String, val priority: Priority) {

    abstract val value: V

    abstract fun <T> accept(visitor: Visitor<T>): T

    data class Id(val hash: String)

    enum class Priority { MAIN, PRIMARY, SECONDARY }

    interface Visitor<out T> {
        fun onIndicatorVisited(property: IndicatorProperty): T
        fun onActionVisited(property: ActionProperty): T
    }
}

class IndicatorProperty(id: Id, name: String, priority: Priority, override val value: Boolean)
    : Property<Boolean>(id, name, priority) {

    override fun <T> accept(visitor: Visitor<T>): T = visitor.onIndicatorVisited(this)
}

class ActionProperty(id: Id, name: String, priority: Priority)
    : Property<Unit>(id, name, priority) {

    override val value: Unit = Unit

    override fun <T> accept(visitor: Visitor<T>): T = visitor.onActionVisited(this)
}

data class BrokerInfo(
        val metadata: Metadata,
        val credentials: BrokerCredentials
) : Serializable

data class BrokerCredentials(
        val host: String,
        val port: Int
) : Serializable