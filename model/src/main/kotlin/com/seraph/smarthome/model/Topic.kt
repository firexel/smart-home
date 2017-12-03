package com.seraph.smarthome.model

/**
 * Created by aleksandr.naumov on 03.12.2017.
 *
 * Suggested topics structure:
 * home/
 *   blocks/
 *     structure/
 *       {block_id} <- Block structures are posted here
 *     outputs/
 *       {block_id}/
 *         {endpoint_ids} <- Measurements are posted here
 *
 */
data class Topic(val segments: List<String>) {
    companion object {
        fun fromString(segments: String): Topic = Topic(segments.split("/"))
    }

    fun subtopic(segments: List<String>): Topic = Topic(this.segments + segments)

    override fun toString(): String = segments.joinToString(separator = "/")
}

class Topics {
    companion object {
        private val rootTopic = Topic(listOf("home"))
        private val blocksTopic = rootTopic.subtopic(listOf("blocks"))

        fun structure(id: Block.Id = Block.Id.any()): Topic
                = blocksTopic.subtopic(listOf("structure", id.hash))

        fun output(block: Block.Id = Block.Id.any(), endpoint: Endpoint.Id = Endpoint.Id.any()): Topic
                = blocksTopic.subtopic(listOf("outputs", block.hash, endpoint.hash))
    }
}