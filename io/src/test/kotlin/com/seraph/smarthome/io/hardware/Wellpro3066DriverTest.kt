package com.seraph.smarthome.io.hardware

import com.nhaarman.mockito_kotlin.mock
import com.seraph.smarthome.device.testing.MockDriverVisitor
import com.seraph.smarthome.util.NoLog
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by aleksandr.naumov on 06.05.18.
 */

class Wellpro3066DriverTest {

    private lateinit var mockScheduler: MockScheduler
    private lateinit var mockVisitor: MockDriverVisitor
    private lateinit var mockSettings: Wellpro3066Driver.Settings

    private val tempSensorsResponseDefault = byteArrayOf(
            0x01, 0x03,
            0x10,
            0x00, 0xF2, 0x27, 0xA9,
            0x00, 0x00, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0x68, 0x9C
    )

    private val tempSensorsResponseAllOffline = byteArrayOf(
            0x01, 0x03,
            0x10,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF,
            0x68, 0x9C
    )

    private val tempSensorsRequest = byteArrayOf(
            0x01, 0x03,
            0x00, 0x00,
            0x00, 0x08,
            0x44, 0x0C
    )

    @Before
    fun setup() {
        mockScheduler = MockScheduler()
        mockVisitor = MockDriverVisitor()
        mockSettings = Wellpro3066Driver.Settings(1)
    }

    @Test
    fun testPerformsRequestImmediatelyAfterConfigure() {
        Wellpro3066Driver(mockScheduler, mockSettings, NoLog()).bind(mockVisitor)
        Assert.assertEquals(1, mockScheduler.postsInQueue)
    }

    @Test
    fun testPublishesDataOnEveryBusReply() {
        mockScheduler.mockResponse(tempSensorsRequest, tempSensorsResponseDefault)

        Wellpro3066Driver(mockScheduler, mockSettings, NoLog()).bind(mockVisitor)

        mockScheduler.proceed()
        mockVisitor.outputs.values.forEach { Assert.assertEquals(1, it.timesInvalidated) }

        mockScheduler.proceed()
        mockVisitor.outputs.values.forEach { Assert.assertEquals(2, it.timesInvalidated) }
    }

    @Test
    fun testCorrectParsingOfTemperatureReadings() {
        mockScheduler.mockResponse(tempSensorsRequest, tempSensorsResponseDefault)

        Wellpro3066Driver(mockScheduler, mockSettings, NoLog()).bind(mockVisitor)
        mockScheduler.proceed()

        assertSensorState(0, 24.2f, true)
        assertSensorState(1, -15.3f, true)
        assertSensorState(2, 0f, true)
        (3 until 8).forEach { assertSensorState(it, 0f, false) }
    }

    @Test
    fun testSensorsGoesOfflineAndBack() {
        Wellpro3066Driver(mockScheduler, mockSettings, NoLog()).bind(mockVisitor)

        mockScheduler.withSingleMock(tempSensorsRequest, tempSensorsResponseDefault) {
            proceed()
            (0..7).forEach { assertSensorOnline(it, it in 0..2) }
        }

        mockScheduler.withSingleMock(tempSensorsRequest, tempSensorsResponseAllOffline) {
            proceed()
            (0..7).forEach { assertSensorOnline(it, false) }
        }

        mockScheduler.withSingleMock(tempSensorsRequest, tempSensorsResponseDefault) {
            proceed()
            (0..7).forEach { assertSensorOnline(it, it in 0..2) }
        }
    }

    private fun assertSensorState(index: Int, temp: Float, online: Boolean) {
        val tempSensor0 = getSensorDeclaration(index)
        Assert.assertEquals(temp, tempSensor0.outputs["value"]!!.value as Float, 0.01f)
        Assert.assertEquals(online, tempSensor0.outputs["online"]!!.value as Boolean)
    }

    private fun assertSensorOnline(index: Int, online: Boolean) {
        val tempSensor0 = getSensorDeclaration(index)
        Assert.assertEquals(online, tempSensor0.outputs["online"]!!.value as Boolean)
    }

    private fun getSensorDeclaration(index: Int) =
            mockVisitor.inners["temp_sensor_$index"]!!
}