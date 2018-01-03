package com.seraph.smarthome.transport.impl

import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttException.*
import java.io.IOException

class MqttOperationException(throwable: Throwable?) : RuntimeException(throwable) {

    private companion object {
        fun reasonCode(throwable: Throwable?): Short = when (throwable) {
            is MqttException -> throwable.reasonCode.toShort()
            is IOException -> REASON_CODE_SERVER_CONNECT_ERROR
            else -> REASON_CODE_CLIENT_EXCEPTION
        }
    }

    val reason: Reason = when (reasonCode(throwable)) {
        REASON_CODE_NO_MESSAGE_IDS_AVAILABLE
        -> Reason.BAD_CLIENT_STATE

        REASON_CODE_INVALID_PROTOCOL_VERSION,
        REASON_CODE_BROKER_UNAVAILABLE,
        REASON_CODE_SUBSCRIBE_FAILED
        -> Reason.BAD_BROKER_STATE

        REASON_CODE_INVALID_CLIENT_ID,
        REASON_CODE_FAILED_AUTHENTICATION,
        REASON_CODE_NOT_AUTHORIZED,
        REASON_CODE_SOCKET_FACTORY_MISMATCH,
        REASON_CODE_SSL_CONFIG_ERROR,
        REASON_CODE_INVALID_MESSAGE
        -> Reason.BAD_CLIENT_CONFIGURATION

        REASON_CODE_CLIENT_TIMEOUT,
        REASON_CODE_WRITE_TIMEOUT,
        REASON_CODE_SERVER_CONNECT_ERROR,
        REASON_CODE_CONNECTION_LOST
        -> Reason.BAD_NETWORK

        REASON_CODE_CLIENT_CONNECTED,
        REASON_CODE_CLIENT_ALREADY_DISCONNECTED,
        REASON_CODE_CLIENT_DISCONNECTING,
        REASON_CODE_CLIENT_NOT_CONNECTED,
        REASON_CODE_CONNECT_IN_PROGRESS,
        REASON_CODE_CLIENT_DISCONNECT_PROHIBITED,
        REASON_CODE_CLIENT_CLOSED,
        REASON_CODE_TOKEN_INUSE,
        REASON_CODE_CLIENT_EXCEPTION,
        REASON_CODE_UNEXPECTED_ERROR,
        REASON_CODE_DISCONNECTED_BUFFER_FULL,
        REASON_CODE_MAX_INFLIGHT
        -> Reason.BAD_CLIENT_LOGIC

        else -> Reason.BAD_CLIENT_LOGIC
    }

    enum class Reason {
        BAD_BROKER_STATE, // should try to reconnect to broker
        BAD_CLIENT_STATE, // should recreate client
        BAD_CLIENT_CONFIGURATION, // should close process and wait for proper configuration
        BAD_CLIENT_LOGIC, // should notify developer about this cases and reboot service
        BAD_NETWORK // should try to reconnect to broker
    }
}