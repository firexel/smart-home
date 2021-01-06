package com.seraph.luxmeter.experiment

import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.domain.Units
import com.seraph.smarthome.domain.impl.Topics
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic

class BrokerPowerSetter(
        private val broker: Broker,
        endpoint: String
) : PowerSetter {

    private val topic: Topic

    init {
        val parts = endpoint.split("/")
        if (parts.size != 2) {
            throw RuntimeException("Endpoint should be /-divided")
        }

        topic = Topics.endpoint(
                Device.Id(parts[0]),
                Endpoint(
                        Endpoint.Id(parts[1]),
                        Types.FLOAT,
                        Endpoint.Direction.INPUT,
                        Endpoint.Retention.NOT_RETAINED,
                        Endpoint.DataKind.CURRENT,
                        Endpoint.Interaction.USER_READONLY,
                        Units.NO
                )
        )
    }

    override fun setPower(powerLevel: Float) {
        broker.publish(topic, Types.FLOAT.serializer.toBytes(powerLevel))
    }
}