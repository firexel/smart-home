package com.seraph.smarthome.io.hardware

import com.seraph.smarthome.device.testing.MockDriverVisitor
import com.seraph.smarthome.device.testing.MockOutput
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by aleksandr.naumov on 08.05.18.
 */
class Wellpro8028DriverTest {

    private val switchesRange = (0..7)

    private lateinit var scheduler: MockScheduler
    private lateinit var visitor: MockDriverVisitor

    @Before
    fun setup() {
        scheduler = MockScheduler()
        visitor = MockDriverVisitor()
    }

    private val switchesRequest = byteArrayOf(
            0x01, 0x02, 0x00, 0x00, 0x00, 0x08, 0x79, 0xcc
    )

    private val switchesResponseThreeOn = byteArrayOf(
            0x01, 0x02, 0x01, 0xC2, 0x20, 0x19
    )

    private val switchesResponseAllOff = byteArrayOf(
            0x01, 0x02, 0x01, 0x00, 0x20, 0x19
    )

    @Test
    fun testStartsCyclicRequestsAfterConfiguration() {
        scheduler.mockResponse(switchesRequest, switchesResponseThreeOn)

        Wellpro8028Driver(scheduler, 0x01).configure(MockDriverVisitor())

        Assert.assertEquals(1, scheduler.postsInQueue)
        scheduler.proceed()

        Assert.assertEquals(1, scheduler.postsInQueue)
    }

    @Test
    fun testCorrectParsingOfSwitchesState() {

        Wellpro8028Driver(scheduler, 0x01).configure(visitor)

        with(scheduler.mockResponse(switchesRequest, switchesResponseThreeOn)) {
            scheduler.proceed()
            assertOutputsInvalidated(1)
            assertOutputsEnabled(1, 6, 7)
            discard()
        }

        with(scheduler.mockResponse(switchesRequest, switchesResponseAllOff)) {
            scheduler.proceed()
            assertOutputsInvalidated(2)
            assertOutputsEnabled(/* none of them */)
            discard()
        }

        with(scheduler.mockResponse(switchesRequest, switchesResponseThreeOn)) {
            scheduler.proceed()
            assertOutputsInvalidated(3)
            assertOutputsEnabled(1, 6, 7)
            discard()
        }
    }

    private fun assertOutputsEnabled(vararg listOfEnabled: Int) {
        switchesRange.forEach {
            val output = getOutput(it)
            Assert.assertEquals(
                    "Wrong state of switch $it",
                    it in listOfEnabled,
                    output.value
            )
        }
    }

    private fun assertOutputsInvalidated(timesInvalidated: Int) {
        switchesRange.forEach {
            val output = getOutput(it)
            Assert.assertEquals(timesInvalidated, output.timesInvalidated)
        }
    }

    private fun getOutput(it: Int) =
            visitor.outputs["switch_$it"]!! as MockOutput<Boolean>
}