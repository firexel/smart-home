package com.seraph.smarthome.transport

import com.seraph.smarthome.model.Device
import com.seraph.smarthome.model.Endpoint
import com.seraph.smarthome.model.Metadata

/**
 * Created by aleksandr.naumov on 03.12.2017.
 *
 * Suggested topics structure:
 * home/
 *   metadata <- Broker name and possible other info stored here
 *   devices/
 *     structure/
 *       {device_id} <- Device structures are posted here
 *     outputs/
 *       {device_id}/
 *         {endpoint_id} <- Values from device outputs are posted here
 *     inputs/
 *       {device_id}/
 *         {endpoint_id} <- Values to device inputs are posted here
 *     properties/
 *       {device_id}/
 *         {endpoint_id} <- Values to device properties are posted here
 *
 */
open class Topic(val segments: List<String>, val persisted: Boolean = true) {
    companion object {
        fun fromString(segments: String): Topic = Topic(segments.split("/"))
    }

    fun subtopic(segments: List<String>): Topic = Topic(this.segments + segments, persisted)

    fun unpersisted(): Topic = Topic(segments, false)

    fun <T> typed(converter: TypedTopic.DataConverter<T>): TypedTopic<T>
            = TypedTopic(segments, persisted, converter)

    override fun toString(): String = segments.joinToString(separator = "/")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Topic
        if (segments != other.segments) return false
        return true
    }

    override fun hashCode(): Int {
        return segments.hashCode()
    }
}

class TypedTopic<T>(
        segments: List<String>,
        persisted: Boolean,
        private val converter: DataConverter<T>)
    : Topic(segments, persisted) {

    fun subscribe(broker: Broker, receiver: (T) -> Unit) {
        broker.subscribe(this) { _, data ->
            try {
                receiver(converter.fromString(data))
            } catch (ex: DataConverter.TypeMismatchException) {
                ex.printStackTrace()
                // TODO("Find a better way to process a error")
            }
        }
    }

    fun publish(broker: Broker, data: T) {
        broker.publish(this, converter.toString(data))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as TypedTopic<*>
        if (converter != other.converter) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + converter.hashCode()
        return result
    }

    interface DataConverter<T> {
        fun fromString(string: String): T
        fun toString(data: T): String

        class TypeMismatchException(msg: String, rootCause: Throwable)
            : RuntimeException(msg, rootCause) {

            constructor(msg: String) : this(msg, RuntimeException())
            constructor(rootCause: Throwable) : this("", rootCause)
        }
    }
}

class Topics {
    companion object {
        private val rootTopic = Topic(listOf("home"))
        private val devicesTopic = rootTopic.subtopic(listOf("devices"))

        fun metadata() = rootTopic
                .subtopic(listOf("metadata"))
                .typed(JsonConverter(Metadata::class))

        fun structure(id: Device.Id = Device.Id.any()) = devicesTopic
                .subtopic(listOf("structure", id.hash))
                .typed(JsonConverter(Device::class))

        fun output(device: Device.Id = Device.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = devicesTopic.subtopic(listOf("outputs", device.hash, endpoint.hash))

        fun input(device: Device.Id = Device.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = devicesTopic.subtopic(listOf("inputs", device.hash, endpoint.hash))

        fun property(device: Device.Id = Device.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = devicesTopic.subtopic(listOf("properties", device.hash, endpoint.hash))
    }
}