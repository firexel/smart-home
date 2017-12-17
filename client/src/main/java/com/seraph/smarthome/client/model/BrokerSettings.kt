package com.seraph.smarthome.client.model

import com.seraph.smarthome.model.Metadata
import java.io.Serializable

data class BrokerInfo(
        val metadata: Metadata,
        val credentials: BrokerCredentials
) : Serializable

data class BrokerCredentials(
        val host: String,
        val port: Int
) : Serializable