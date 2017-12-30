package com.seraph.smarthome.server

import com.seraph.smarthome.model.ConnectionsList
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topics

/**
 * Created by aleksandr.naumov on 02.12.2017.
 */

class Connector(
        private val broker: Broker,
        private val connections: ConnectionsList) {

    public fun serve() {
        connections.list.forEach {
            broker.subscribe(Topics.output(it.from.device, it.from.endpoint)) { _, data ->
                broker.publish(Topics.input(it.to.device, it.to.endpoint), data)
            }
        }
    }
}