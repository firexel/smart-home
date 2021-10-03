package com.seraph.smarthome.wirenboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select

suspend fun <T> Flow<T>.firstOrDefault(def: T): T {
    return firstOrNull() ?: def
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.timeout(
        timeoutDelay: Long
): Flow<T> {
    val upstreamFlow = this

    return flow {
        val collector = this

        // create new scope to create values channel in it
        // and to confine all the children coroutines there
        coroutineScope {

            // reroute original flow values into a channel that will be part of select clause
            val valuesChannel = produce {
                upstreamFlow.collect { value ->
                    send(TimeoutState.Value(value))
                }
                close()
            }

            // run in the loop until we get a confirmation that flow has ended
            var latestValue: TimeoutState<T> = TimeoutState.Initial()
            while (latestValue !is TimeoutState.Final) {
                latestValue = select {
                    onTimeout(timeoutDelay) {
                        TimeoutState.Final.Timeout()
                    }
                    try {
                        valuesChannel.onReceive { it }
                    } catch (ex: ClosedReceiveChannelException) {
                        TimeoutState.Final.Done<T>()
                    }
                }
                if (latestValue is TimeoutState.Value) {
                    collector.emit(latestValue.value)
                }
            }

            if (latestValue is TimeoutState.Final.Timeout) {
                valuesChannel.cancel()
            }
        }
    }
}

private sealed class TimeoutState<T> {
    sealed class Final<T> : TimeoutState<T>() {
        class Done<T> : Final<T>()
        class Timeout<T> : Final<T>()
    }

    class Initial<T> : TimeoutState<T>()
    data class Value<T>(val value: T) : TimeoutState<T>()
}

class TimeoutException : RuntimeException("Timed out waiting for emission")