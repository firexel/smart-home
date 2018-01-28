package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Topic

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal interface Client {
    var disconnectionCallback: ((ClientException) -> Unit)?

    fun connect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit)
    fun disconnect(onSuccess: () -> Unit, onFail: (ClientException) -> Unit)

    fun subscribe(topic: Topic, listener: (topic: Topic, data: ByteArray) -> Unit)
    fun publish(topic: Topic, data: ByteArray)
}