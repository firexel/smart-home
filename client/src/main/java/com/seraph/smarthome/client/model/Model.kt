package com.seraph.smarthome.client.model

import com.seraph.smarthome.domain.Metainfo
import java.io.Serializable

/**
 * Created by aleksandr.naumov on 29.12.17.
 */

data class BrokerInfo(
        val metainfo: Metainfo,
        val credentials: BrokerCredentials
) : Serializable

data class BrokerCredentials(
        val host: String,
        val port: Int
) : Serializable