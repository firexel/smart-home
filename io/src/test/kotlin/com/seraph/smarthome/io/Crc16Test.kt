package com.seraph.smarthome.io

import com.seraph.smarthome.io.hardware.Crc16
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Created by aleksandr.naumov on 24.12.17.
 */
class Crc16Test {
    private lateinit var crc: Crc16

    @Before
    fun setup() {
        crc = Crc16()
    }

    @Test
    fun test8Bytes() {
        byteArrayOf(0x01, 0x0f, 0x00, 0x00, 0x00, 0x08, 0x01, 0x00).forEach {
            crc.update(it)
        }
        assertSumEquals(0x95fe)
    }

    @Test
    fun test6Bytes() {
        byteArrayOf(0x01, 0x05, 0x00, 0x00, 0xff.toByte(), 0x00).forEach {
            crc.update(it)
        }
        assertSumEquals(0x3a8c)
    }

    private fun assertSumEquals(expected: Int) {
        val actual = crc.checksum.toInt()
        assertEquals(
                "Expected ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)} instead",
                expected.and(0xffff), actual.and(0xffff)
        )
    }
}