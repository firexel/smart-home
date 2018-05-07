package com.seraph.smarthome.io.hardware

import com.nhaarman.mockito_kotlin.*
import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.Types
import org.junit.Assert
import org.junit.Test

/**
 * Created by aleksandr.naumov on 06.05.18.
 */

class Wellpro3066DriverTest {
    @Test
    fun testPerformsRequestImmediatelyAfterConfigure() {
        val mockScheduler: Scheduler = mock()
        val mockVisitor: DeviceDriver.Visitor = mock {
            on(it.declareOutput(any(), same(Types.FLOAT), any()))
                    .thenReturn(mock())
        }

        Wellpro3066Driver(1, mockScheduler).configure(mockVisitor)
        verify(mockScheduler, times(1)).post(
                any<Bus.Command<*>>(),
                eq(0L),
                any()
        )
        verifyNoMoreInteractions(mockScheduler)
    }

    @Test
    fun testPublishesDataOnEveryBusReply() {
        val mockScheduler = MockScheduler()
        mockScheduler.mockResponse(
                request = byteArrayOf(
                        0x01, 0x03,
                        0x00, 0x00,
                        0x00, 0x08,
                        0x44, 0x0C),
                response = byteArrayOf(
                        0x01, 0x03,
                        0x10,
                        0x00, 0xF2,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0x68, 0x9C
                )
        )

        val outputs = (0 until 8).map { mock<DeviceDriver.Output<Float>>() }
        val visitor: DeviceDriver.Visitor = mock { visitor ->
            outputs.forEachIndexed { index, output ->
                on(visitor.declareOutput(eq("temp_sensor_$index"), same(Types.FLOAT), any()))
                        .thenReturn(output)
            }
        }

        outputs.forEach { verify(it, never()).invalidate() }
        Wellpro3066Driver(1, mockScheduler).configure(visitor)

        mockScheduler.proceed()
        outputs.forEach { verify(it, times(1)).invalidate() }

        mockScheduler.proceed()
        outputs.forEach { verify(it, times(2)).invalidate() }
    }

    @Test
    fun testCorrectParsingOfTemperatureReadings() {
        val mockScheduler = MockScheduler()
        mockScheduler.mockResponse(
                request = byteArrayOf(
                        0x01, 0x03,
                        0x00, 0x00,
                        0x00, 0x08,
                        0x44, 0x0C),
                response = byteArrayOf(
                        0x01, 0x03,
                        0x10,
                        0x00, 0xF2,
                        0x27, 0xA9,
                        0x00, 0x00,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0xFF, 0xFF,
                        0x68, 0x9C
                )
        )

        val outputs = (0 until 8).map { MockOutput<Float>() }
        val visitor: DeviceDriver.Visitor = mock { visitor ->
            outputs.forEachIndexed { index, output ->
                on(visitor.declareOutput(eq("temp_sensor_$index"), same(Types.FLOAT), any()))
                        .thenReturn(output)
            }
        }

        Wellpro3066Driver(1, mockScheduler).configure(visitor)
        mockScheduler.proceed()
        Assert.assertEquals(24.2f, outputs[0].value , 0.01f)
        Assert.assertEquals(-15.3f, outputs[1].value , 0.01f)
        outputs.drop(2).forEach { Assert.assertEquals(0f, it.value, 0.001f) }
    }
}