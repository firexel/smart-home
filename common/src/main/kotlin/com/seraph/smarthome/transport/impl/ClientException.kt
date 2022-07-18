package com.seraph.smarthome.transport.impl

internal open class ClientException(val reason: Reason, throwable: Throwable? = null)
    : RuntimeException(throwable) {

    enum class Reason {
        BAD_BROKER_STATE, // should try to reconnect to broker
        BAD_CLIENT_STATE, // should recreate client
        BAD_CLIENT_CONFIGURATION, // should close process and wait for proper com.seraph.connector.configuration
        BAD_CLIENT_LOGIC, // should notify developer about this cases and reboot service
        BAD_NETWORK // should try to reconnect to broker
    }
}