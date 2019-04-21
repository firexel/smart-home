package com.seraph.smarthome.transport

data class Topic(val segments: List<String>, val persisted: Boolean = true) {
    companion object {
        fun fromString(segments: String): Topic = Topic(segments.split("/"))
    }

    constructor(vararg segments: String) : this(segments.asList())

    fun subtopic(segment: String): Topic = Topic(this.segments + segment, persisted)

    fun retained(isPersisted: Boolean): Topic = Topic(segments, isPersisted)

    override fun toString(): String = segments.joinToString(separator = "/")
}