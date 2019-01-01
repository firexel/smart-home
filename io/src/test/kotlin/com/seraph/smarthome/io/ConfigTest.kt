package com.seraph.smarthome.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class ConfigTest {

    @Test
    fun readEmptyConfig() {
        val config = readConfig(StringReader("{'buses':{}}"))
        assertTrue(config.buses.isEmpty())
    }

    @Test
    fun readBusWithNoDevicesOnIt() {
        val config = readConfig(StringReader("{" +
                "'buses':{" +
                "  'foo_bus': {" +
                "     'settings': {" +
                "        'path': '/dev/bar'," +
                "        'baud_rate': 777," +
                "        'parity': 'NO'," +
                "        'data_bits': 8," +
                "        'stop_bits': 1" +
                "      }," +
                "      'devices': {}" +
                "  }" +
                "}" +
                "}"))
        assertEquals(1, config.buses.size)
        val expectedSettings = PortSettingsNode("/dev/bar", 777, ParityNode.NO, 8, 1)
        val actualBus = config.buses["foo_bus"]!!
        assertEquals(expectedSettings, actualBus.settings)
        assertTrue(actualBus.devices.isEmpty())
    }

    @Test
    fun readBusWithWellpro8028OnIt() {
        val config = readConfig(StringReader("{" +
                "'buses':{" +
                "  'foo_bus': {" +
                "     'settings': {" +
                "        'path': '/dev/bar'," +
                "        'baud_rate': 777," +
                "        'parity': 'NO'," +
                "        'data_bits': 8," +
                "        'stop_bits': 1" +
                "      }," +
                "      'devices': {" +
                "        'test_device': {" +
                "          'driver': 'WELLPRO_8028'," +
                "          'settings': {" +
                "            'address_at_bus': 7" +
                "          }," +
                "          'connections': {" +
                "            'DI_02': 'foo'," +
                "            'DO_08': ['bar', 'boob']" +
                "          }" +
                "        }" +
                "      }" +
                "  }" +
                "}" +
                "}"))

        assertEquals(1, config.buses.size)

        val actualBus = config.buses["foo_bus"]!!
        val expectedDevice = DeviceNode(
                DeviceDriverNameNode.WELLPRO_8028,
                ModbusDeviceSettingsNode(0x07),
                mapOf("DI_02" to AliasNode(listOf("foo")), "DO_08" to AliasNode(listOf("bar", "boob")))
        )
        val actualDevice = actualBus.devices["test_device"]!!
        assertEquals(expectedDevice, actualDevice)
    }
}