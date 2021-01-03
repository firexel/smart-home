package com.seraph.smarthome.client.repositories

import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.util.NetworkMonitor

interface NetworkRepository {
    val network: Network
    val monitor: NetworkMonitor
}