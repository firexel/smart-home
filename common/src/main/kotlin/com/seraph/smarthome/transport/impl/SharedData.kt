package com.seraph.smarthome.transport.impl

import org.eclipse.paho.client.mqttv3.MqttAsyncClient

data class SharedData(
        val client: MqttAsyncClient,
        val actions: List<(MqttAsyncClient) -> Unit>,
        val state: State,
        val timesRetried:Int
)