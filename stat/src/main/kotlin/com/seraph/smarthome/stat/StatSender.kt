package com.seraph.smarthome.stat

interface StatSender {

    fun enqueue(name: MetricName, value: Double, timestamp: Long)

    data class MetricName(val segments: List<String>) {
        override fun toString(): String {
            return segments.joinToString(separator = ".")
        }
    }
}