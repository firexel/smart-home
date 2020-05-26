package com.seraph.smarthome.domain.impl

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.transport.Topic

/**
 * Created by aleksandr.naumov on 03.12.2017.
 *
 * Suggested topics structure:
 * home/
 *   metadata <- Broker name and possible other info stored here
 *   devices/
 *     structure/
 *       {device_id} <- Device structures are posted here
 *     endpoints/
 *       {device_id}/
 *         {endpoint_id} <- Values from and to device endpoints are posted here
 */
class Topics {
    companion object {
        private val rootTopic = Topic(listOf("home"))
        private val devicesTopic = rootTopic.subtopic("devices")

        fun metadata() = rootTopic
                .subtopic("metadata")

        fun structure(id: Device.Id? = null) = devicesTopic
                .subtopic("structure")
                .subtopic(id?.value ?: "+")

        fun endpoint(device: Device.Id, endpoint: Endpoint<*>): Topic = devicesTopic
                .subtopic("endpoints")
                .subtopic(device.value)
                .subtopic(endpoint.id.value)
                .retained(endpoint.retention == Endpoint.Retention.RETAINED)
    }
}