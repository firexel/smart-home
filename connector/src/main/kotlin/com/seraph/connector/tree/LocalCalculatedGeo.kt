package com.seraph.connector.tree

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator
import com.luckycatlabs.sunrisesunset.dto.Location
import script.definition.Geo
import java.time.LocalDateTime
import java.util.*


class LocalCalculatedGeo(
    lat: String,
    lon: String,
    timezone: String,
    private val clock: Clock = SystemClock()

) : Geo {

    private val timeZone = TimeZone.getTimeZone(timezone)
    private val calculator = SunriseSunsetCalculator(Location(lat, lon), timeZone.id)

    override val todaySunriseTime: LocalDateTime
        get() = calculator
            .getOfficialSunriseCalendarForDate(Calendar.getInstance())
            .toLocalDateTime()

    override val todaySunsetTime: LocalDateTime
        get() = calculator
            .getOfficialSunsetCalendarForDate(Calendar.getInstance())
            .toLocalDateTime()

    private fun Calendar.toLocalDateTime() =
        LocalDateTime.ofInstant(toInstant(), timeZone.toZoneId())

    interface Clock {
        fun now(): LocalDateTime
    }

    class SystemClock : Clock {
        override fun now(): LocalDateTime = LocalDateTime.now()
    }
}