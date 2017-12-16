package com.seraph.smarthome.model

/**
 * Created by aleksandr.naumov on 03.12.2017.
 *
 * Suggested topics structure:
 * home/
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
data class Topic(private val segments: List<String>) {
    companion object {
        fun fromString(segments: String): Topic = Topic(segments.split("/"))
    }

    fun subtopic(segments: List<String>): Topic = Topic(this.segments + segments)

    override fun toString(): String = segments.joinToString(separator = "/")
}

class Topics {
    companion object {
        private val rootTopic = Topic(listOf("home"))
        private val blocksTopic = rootTopic.subtopic(listOf("devices"))

        fun structure(id: Device.Id = Device.Id.any()): Topic
                = blocksTopic.subtopic(listOf("structure", id.hash))

        fun output(device: Device.Id = Device.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = blocksTopic.subtopic(listOf("outputs", device.hash, endpoint.hash))

        fun input(device: Device.Id = Device.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = blocksTopic.subtopic(listOf("inputs", device.hash, endpoint.hash))

        fun property(device: Device.Id = Device.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = blocksTopic.subtopic(listOf("properties", device.hash, endpoint.hash))
    }
}