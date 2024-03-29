package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import script.definition.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ClockNode(
    private val interval: Clock.Interval,
) : Node, Clock {

    override val time: StateFlowProducerNode<LocalDateTime> =
        StateFlowProducerNode(this, LocalDateTime.now())

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                time.value = now
                val nextHourTick = LocalDateTime.now().let {
                    when (interval) {
                        Clock.Interval.HOUR ->
                            it.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                        Clock.Interval.MINUTE ->
                            it.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
                        Clock.Interval.SECOND ->
                            it.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1)
                    }
                }

                val diff = nextHourTick.toInstant(ZoneOffset.UTC).toEpochMilli() -
                        now.toInstant(ZoneOffset.UTC).toEpochMilli()

                delay(diff)
            }
        }
    }
}