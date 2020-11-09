package com.seraph.smarthome.stat

/**
 * Sends metrics to a graphite using plaintext protocol over tcp
 */
class GraphiteStatSender(host: String, port: Int) : StatSender {
    override fun enqueue(name: StatSender.MetricName, value: Double, timestamp: Long) {

    }
}