package com.seraph.connector.tree

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import script.definition.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Suppress("UNCHECKED_CAST")
internal class ClockNodeTest {

    @Test
    fun testSecondTick() = runTest {
        val testStart = LocalDateTime.now()
        val node = ClockNode(Clock.Interval.SECOND)
        val ticks = mutableListOf<LocalDateTime?>()
        launch { (node.time as Node.Producer<LocalDateTime>).flow.toCollection(ticks) }
        launch { node.run(this) }
        val runTime = LocalDateTime.now()
        delay(2000)
        ticks.waitFor(
            20, INACCURATE, listOf(
                testStart,
                runTime,
                runTime.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1),
                runTime.truncatedTo(ChronoUnit.SECONDS).plusSeconds(2)
            )
        )
    }

    val INACCURATE: (LocalDateTime?, LocalDateTime?) -> Boolean =
        { a, b ->
            abs(
                a!!.toInstant(ZoneOffset.UTC).toEpochMilli()
                        - b!!.toInstant(ZoneOffset.UTC).toEpochMilli()
            ) < 60
        }
}