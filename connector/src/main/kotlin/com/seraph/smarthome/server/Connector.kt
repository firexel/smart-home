package com.seraph.smarthome.server

import com.seraph.smarthome.model.Broker
import com.seraph.smarthome.model.ConnectionsList
import com.seraph.smarthome.model.Topics

/**
 * Created by aleksandr.naumov on 02.12.2017.
 */

class Connector(
        private val broker: Broker,
        private val connections: ConnectionsList) {

    public fun serve() {
        connections.list.forEach {
            broker.subscribe(Topics.output(it.from.device, it.from.endpoint)) { _, data ->
                broker.publish(Topics.output(it.to.device, it.to.endpoint), data)
            }
        }
    }
}