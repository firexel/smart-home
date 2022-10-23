package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import script.definition.Monitor
import java.time.Clock
import java.util.*

class MonitorNode<T>(
    private val timeWindowMs: Long,
    private val aggregator: (List<T>) -> T?,
    private val clock: Clock
) : Monitor<T> {

    override val output = StateFlowProducerNode<T>(this, null)
    override val input = StateFlowConsumerNode<T>(this, null)

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            val records = LinkedList<RecordedValue<T>>()
            input.stateFlow.collect { nextValue ->
                if (nextValue != null) {
                    val now = clock.millis()
                    records.add(RecordedValue(nextValue, now))
                    records.trim(now)
                    updateOutput(records)
                }
            }
        }
    }

    private fun updateOutput(records: LinkedList<RecordedValue<T>>) {
        if (records.isNotEmpty()) {
            aggregator(records.map { it.data })?.let { aggregated ->
                output.value = aggregated
            }
        }
    }

    private fun LinkedList<RecordedValue<T>>.trim(now: Long) {
        while (isNotEmpty() && first.timeRecorded < now - timeWindowMs) {
            removeFirst()
        }
    }

    private data class RecordedValue<T>(
        val data: T,
        val timeRecorded: Long
    )
}