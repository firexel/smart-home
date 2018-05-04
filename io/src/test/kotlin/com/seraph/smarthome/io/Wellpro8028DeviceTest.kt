package com.seraph.smarthome.io

import com.seraph.smarthome.io.hardware.asHexString
import junit.framework.AssertionFailedError
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Created by aleksandr.naumov on 24.12.17.
 */
class Wellpro8028DeviceTest {
    private lateinit var connection: MockConnection
    private lateinit var device: Wellpro8028Device

    @Before
    fun setup() {
        connection = MockConnection()
        device = Wellpro8028Device(connection, 0x01)
    }

    @Test
    fun testEnablingOutput0() {
        device.setRelayState(0, true)
        connection.assertCommandWas(byteArrayOf(
                0x01, 0x05, 0x00, 0x00, 0xff.toByte(), 0x00, 0x8c.toByte(), 0x3a
        ))
    }

    @Test
    fun testDisablingOutput0() {
        device.setRelayState(0, false)
        connection.assertCommandWas(byteArrayOf(
                0x01, 0x05, 0x00, 0x00, 0x00, 0x00, 0xcd.toByte(), 0xca.toByte()
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOperatingPortOutOfRange() {
        device.setRelayState(10, true)
    }

    private class MockConnection {
        private var commandBytes: ByteArray? = null

        override fun send(byteArray: ByteArray, responseSize:Int):ByteArray {
            if (commandBytes == null) {
                commandBytes = byteArray
            } else {
                throw AssertionFailedError("2 commands fired in one test")
            }
            return byteArray
        }

        fun assertCommandWas(bytes: ByteArray) {
            val log = commandBytes
            if (log != null) {
                assertEquals(
                        "Lengths are not equals. Expecting ${bytes.size}, got ${log.size} instead",
                        bytes.size, log.size
                )
                val expectedHexes = bytes.asHexString()
                val actualHexes = log.asHexString()
                assertEquals("Command bytes are not equals", expectedHexes, actualHexes)
            }
        }
    }
}