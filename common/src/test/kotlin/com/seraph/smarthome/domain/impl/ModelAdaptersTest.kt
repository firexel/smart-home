package com.seraph.smarthome.domain.impl

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.seraph.smarthome.domain.*
import org.junit.Assert
import org.junit.Test

/**
 * Created by aleksandr.naumov on 21.01.18.
 */
internal class ModelAdaptersTest {

    private val gson = with(GsonBuilder()) {
        installModelAdapters(this)
        create()
    }

    @Test
    fun testEmptyDevice() {
        val expected = Device(Device.Id(listOf("test")), emptyList())
        assertEqualsAfterWriteAndRead(expected)
    }

    @Test
    fun testEmptySecondLevelDevice() {
        val expected = Device(Device.Id(listOf("test", "inner")), emptyList())
        assertEqualsAfterWriteAndRead(expected)
    }

    @Test
    fun testDeviceWithOneEndpoint() {
        val endpoint = Endpoint(
                Endpoint.Id("test"),
                Types.BOOLEAN,
                Endpoint.Direction.INPUT,
                Endpoint.Retention.RETAINED,
                Endpoint.DataKind.CURRENT,
                Endpoint.Interaction.USER_EDITABLE,
                Units.GRAMS
        )
        val expected = Device(Device.Id(listOf("test")), listOf(endpoint))
        assertEqualsAfterWriteAndRead(expected)
    }

    private fun assertEqualsAfterWriteAndRead(expected: Device) {
        val actual = writeAndRead(expected)
        Assert.assertEquals(expected, actual)
    }

    private fun writeAndRead(expected: Device): Device {
        val serialized: String = gson.toJson(expected)
        return gson.fromJson(serialized, Device::class.java)
    }
}