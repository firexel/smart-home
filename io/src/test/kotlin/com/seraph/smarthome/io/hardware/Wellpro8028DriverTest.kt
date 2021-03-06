package com.seraph.smarthome.io.hardware

import com.seraph.smarthome.device.ValidationException
import com.seraph.smarthome.device.testing.MockDriverVisitor
import com.seraph.smarthome.device.testing.MockInput
import com.seraph.smarthome.device.testing.MockOutput
import com.seraph.smarthome.util.NoLog
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by aleksandr.naumov on 08.05.18.
 */
class Wellpro8028DriverTest {

    private val switchesRange = (1..8)

    private lateinit var scheduler: MockScheduler
    private lateinit var visitor: MockDriverVisitor
    private lateinit var configuration: Wellpro8028Driver.Settings

    @Before
    fun setup() {
        scheduler = MockScheduler()
        visitor = MockDriverVisitor()
        configuration = Wellpro8028Driver.Settings(
                0x01,
                switchesRange.map { "DI_0$it" to "test_di_$it" }.toMap() +
                        switchesRange.map { "DO_0$it" to "test_do_$it" }.toMap()
        )
    }

    private val relaySetRequest = byteArrayOf(
            0x01, 0x05, 0x00, 0x00, 0xFF, 0x00, 0x8C, 0x3A
    )

    private val relaySetResponse = relaySetRequest

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

        Wellpro8028Driver(scheduler, configuration, NoLog())
                .bind(MockDriverVisitor())

        Assert.assertEquals(1, scheduler.postsInQueue)
        scheduler.proceed()

        Assert.assertEquals(1, scheduler.postsInQueue)
    }

    @Test
    fun testCorrectParsingOfSwitchesState() {

        Wellpro8028Driver(scheduler, configuration, NoLog())
                .bind(visitor)

        scheduler.withSingleMock(switchesRequest, switchesResponseThreeOn) {
            proceed()
            assertOutputsInvalidated(1)
            assertOutputsEnabled(2, 7, 8)
        }

        scheduler.withSingleMock(switchesRequest, switchesResponseAllOff) {
            proceed()
            assertOutputsInvalidated(2)
            assertOutputsEnabled(/* none of them */)
        }

        scheduler.withSingleMock(switchesRequest, switchesResponseThreeOn) {
            proceed()
            assertOutputsInvalidated(3)
            assertOutputsEnabled(2, 7, 8)
        }
    }

    @Test
    fun testDeviceRelaysUpdate() {

        Wellpro8028Driver(scheduler, configuration, NoLog())
                .bind(visitor)

        val input1 = visitor.inputs["test_do_1"] as MockInput<Boolean>

        Assert.assertEquals(1, scheduler.postsInQueue)
        input1.post(true)
        Assert.assertEquals(2, scheduler.postsInQueue)

        scheduler.withSingleMock(switchesRequest, switchesResponseAllOff) {
            proceed()
            Assert.assertEquals(2, scheduler.postsInQueue)
        }

        scheduler.withSingleMock(relaySetRequest, relaySetResponse) {
            proceed()
            Assert.assertEquals(1, scheduler.postsInQueue)
        }
    }

    @Test(expected = ValidationException::class)
    fun testDeviceInitWithInvalidConfig_DI_OutOfRange() {
        Wellpro8028Driver(scheduler, makeInvalidConfig("DI_09"), NoLog())
    }

    @Test(expected = ValidationException::class)
    fun testDeviceInitWithInvalidConfig_DO_OutOfRange() {
        Wellpro8028Driver(scheduler, makeInvalidConfig("DO_09"), NoLog())
    }

    @Test(expected = ValidationException::class)
    fun testDeviceInitWithInvalidConfig_DI_Zero() {
        Wellpro8028Driver(scheduler, makeInvalidConfig("DI_00"), NoLog())
    }

    @Test(expected = ValidationException::class)
    fun testDeviceInitWithInvalidConfig_DO_Zero() {
        Wellpro8028Driver(scheduler, makeInvalidConfig("DO_00"), NoLog())
    }

    @Test(expected = ValidationException::class)
    fun testDeviceInitWithInvalidConfig_DO_Misformat() {
        Wellpro8028Driver(scheduler, makeInvalidConfig("DO01"), NoLog())
    }

    @Test(expected = ValidationException::class)
    fun testDeviceInitWithInvalidConfig_UnknownShit() {
        Wellpro8028Driver(scheduler, makeInvalidConfig("invalid"), NoLog())
    }

    private fun makeInvalidConfig(invalidIoName: String): Wellpro8028Driver.Settings {
        return Wellpro8028Driver.Settings(
                0x01,
                mapOf(invalidIoName to "test_invalid")
        )
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
            visitor.outputs["test_di_$it"]!! as MockOutput<Boolean>
}