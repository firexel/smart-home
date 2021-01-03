package com.seraph.smarthome.client.app

import com.seraph.smarthome.client.repositories.NetworkRepository
import com.seraph.smarthome.util.Log

interface Services {
    val networkRepository: NetworkRepository
    val log: Log
}