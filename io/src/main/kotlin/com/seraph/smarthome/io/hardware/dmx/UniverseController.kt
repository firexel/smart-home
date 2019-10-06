package com.seraph.smarthome.io.hardware.dmx

import com.seraph.smarthome.io.hardware.dmx.ola.DmxSession
import com.seraph.smarthome.util.Log

class UniverseController(private val session: DmxSession, private val log: Log) {

    private val fixtures = arrayOfNulls<Fixture?>(512)
    private val frameTimeMs = 10L // 100fps

    fun addFixture(fixture: Fixture, busIndex: Int) {
        val existedFixture = try {
            if (busIndex == 0) throw ArrayIndexOutOfBoundsException("0 address is prohibited")
            fixtures[busIndex - 1]
        } catch (ex: ArrayIndexOutOfBoundsException) {
            log.w("Cannot insert fixture at index $busIndex")
        }
        if (existedFixture != null) {
            log.w("Replacing fixture $existedFixture")
        }
        fixtures[busIndex - 1] = fixture
    }

    fun start() {
        Thread {
            var startNanos = System.nanoTime()
            while (true) {
                Thread.sleep(frameTimeMs)
                val nanosNow = System.nanoTime()
                loop(nanosNow - startNanos)
                startNanos = nanosNow
            }
        }.start()
    }

    private fun loop(nanosPassed: Long) {
        sendDmx(update(nanosPassed))
    }

    private fun update(nanosPassed: Long): Int {
        var lastFixtureIndex = 0
        for (i in 0 until fixtures.size) {
            val fixture = fixtures[i]
            if (fixture != null) {
                fixture.update(nanosPassed)
                lastFixtureIndex = i
            }
        }
        return lastFixtureIndex
    }

    private fun sendDmx(lastFixtureIndex: Int) {
        val values = ShortArray(lastFixtureIndex + 1) { i ->
            Math.round((fixtures[i]?.value ?: 0f) * 255).toShort()
        }
        session.sendDmx(values)
    }

    interface Fixture {
        fun update(nanosPassed: Long)
        val value: Float
    }

}