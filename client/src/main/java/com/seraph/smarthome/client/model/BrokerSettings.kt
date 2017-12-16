package com.seraph.smarthome.client.model

import java.io.Serializable

data class BrokerSettings (
        val id:Int = 0,
        val host: String,
        val port: Int
) : Serializable