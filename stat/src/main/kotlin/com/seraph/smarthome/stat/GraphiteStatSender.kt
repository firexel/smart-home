package com.seraph.smarthome.stat

import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.ManagedConnection

/**
 * Sends metrics to a graphite using plaintext protocol over tcp
 */
class GraphiteStatSender(host: String, port: Int, private val log: Log) : StatSender {

    private val connection = ManagedConnection(host, port, log.copy("Connection"))

    override fun enqueue(name: StatSender.MetricName, value: Double, timestamp: Long) {
        val metricValue = if (value.isNaN()) {
            "None"
        } else {
            value.toString()
        }
        connection.send("${name.toGraphiteFormat()} $metricValue $timestamp\n")
    }

    private fun StatSender.MetricName.toGraphiteFormat(): String {
        return this.segments.joinToString(separator = ".")
    }
}