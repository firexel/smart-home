package com.seraph.connector.tree

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import script.definition.Clock
import script.definition.Producer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ClockNode(
    private val interval: Clock.Interval,
) : Node, Clock {

    private val state = MutableStateFlow(LocalDateTime.now())

    override val time: Producer<LocalDateTime> =
        object : Producer<LocalDateTime>, Node.Producer<LocalDateTime> {
            override val parent: Node
                get() = this@ClockNode
            override val flow: Flow<LocalDateTime?>
                get() = state
        }

    override suspend fun run(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                state.value = now
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