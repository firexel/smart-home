package com.seraph.smarthome.transport.impl

import org.eclipse.paho.client.mqttv3.MqttException
import java.io.IOException

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal class PahoClientException(private val throwable: Throwable?) : ClientException(
        inferReason(reasonCode(throwable)), throwable) {

    override val message: String?
        get() = if (throwable == null) {
            "Unknown error"
        } else if (throwable is MqttException) {
            "${throwable::class.simpleName} error with code ${throwable.reasonCode} " +
                    "and message \"${throwable.message}\""
        } else {
            "${throwable::class.simpleName} error with message \"${throwable.message}\""
        } + " (treated as $reason)"

    private companion object {
        fun reasonCode(throwable: Throwable?): Short = when (throwable) {
            is MqttException -> throwable.reasonCode.toShort()
            is IOException -> MqttException.REASON_CODE_SERVER_CONNECT_ERROR
            else -> MqttException.REASON_CODE_CLIENT_EXCEPTION
        }

        fun inferReason(reasonCode: Short): Reason = when (reasonCode) {
            MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE
            -> Reason.BAD_CLIENT_STATE

            MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION,
            MqttException.REASON_CODE_BROKER_UNAVAILABLE,
            MqttException.REASON_CODE_SUBSCRIBE_FAILED
            -> Reason.BAD_BROKER_STATE

            MqttException.REASON_CODE_INVALID_CLIENT_ID,
            MqttException.REASON_CODE_FAILED_AUTHENTICATION,
            MqttException.REASON_CODE_NOT_AUTHORIZED,
            MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH,
            MqttException.REASON_CODE_SSL_CONFIG_ERROR,
            MqttException.REASON_CODE_INVALID_MESSAGE
            -> Reason.BAD_CLIENT_CONFIGURATION

            MqttException.REASON_CODE_CLIENT_TIMEOUT,
            MqttException.REASON_CODE_WRITE_TIMEOUT,
            MqttException.REASON_CODE_SERVER_CONNECT_ERROR,
            MqttException.REASON_CODE_CONNECTION_LOST
            -> Reason.BAD_NETWORK

            MqttException.REASON_CODE_CLIENT_CONNECTED,
            MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED,
            MqttException.REASON_CODE_CLIENT_DISCONNECTING,
            MqttException.REASON_CODE_CLIENT_NOT_CONNECTED,
            MqttException.REASON_CODE_CONNECT_IN_PROGRESS,
            MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED,
            MqttException.REASON_CODE_CLIENT_CLOSED,
            MqttException.REASON_CODE_TOKEN_INUSE,
            MqttException.REASON_CODE_CLIENT_EXCEPTION,
            MqttException.REASON_CODE_UNEXPECTED_ERROR,
            MqttException.REASON_CODE_DISCONNECTED_BUFFER_FULL,
            MqttException.REASON_CODE_MAX_INFLIGHT
            -> Reason.BAD_CLIENT_LOGIC

            else -> Reason.BAD_CLIENT_LOGIC
        }
    }
}