package com.seraph.connector.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class LocalCalculatedGeoTest {

    @Test
    fun testSunriseCalculation() {
        val geo = LocalCalculatedGeo(
            "55.7520233",
            "37.6153107",
            "Europe/Moscow",
            FixedClock()
        )
        assertEquals(geo.todaySunriseTime, LocalDateTime.parse("2022-12-25T08:59"))
    }

    @Test
    fun getTodaySunsetTime() {
        val geo = LocalCalculatedGeo(
            "55.7520233",
            "37.6153107",
            "Europe/Moscow",
            FixedClock()
        )
        assertEquals(geo.todaySunsetTime, LocalDateTime.parse("2022-12-25T16:00"))
    }

    class FixedClock : LocalCalculatedGeo.Clock {
        override fun now(): LocalDateTime = LocalDateTime.parse("2022-12-25")
    }
}