package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


@Suppress("ArrayInDataClass")
data class Publication(val topic: Topic, val data: ByteArray)

@OptIn(ExperimentalCoroutinesApi::class)
fun Broker.subscribeAsFlow(topic: Topic): Flow<Publication> {
    val broker = this
    return callbackFlow {
        val sub = broker.subscribe(topic) { t, p ->
            trySendBlocking(Publication(t, p))
        }
        awaitClose {
            sub.unsubscribe()
        }
    }
}