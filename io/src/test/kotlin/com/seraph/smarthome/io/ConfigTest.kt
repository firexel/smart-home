package com.seraph.smarthome.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

fun testCatalogue(name: String): DriverInfo {
    if (name == "test_driver") {
        return DriverInfo(TestDeviceSettings::class)
    } else {
        throw UnsupportedOperationException()
    }
}

data class TestDeviceSettings(val value: Int)

class ConfigTest {

    @Test
    fun readEmptyConfig() {
        val config = readConfig(StringReader("{'buses':{}}"), ::testCatalogue)
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
                "}"), ::testCatalogue)
        assertEquals(1, config.buses.size)
        val expectedSettings = PortSettingsNode("/dev/bar", 777, ParityNode.NO, 8, 1)
        val actualBus = config.buses["foo_bus"]!!
        assertEquals(expectedSettings, actualBus.settings)
        assertTrue(actualBus.devices.isEmpty())
    }

    @Test
    fun readBusWithWellpro8028OnIt() {
        val configString = "{" +
                "'buses':{\n" +
                "  'foo_bus': {\n" +
                "     'settings': {\n" +
                "        'path': '/dev/bar',\n" +
                "        'baud_rate': 777,\n" +
                "        'parity': 'NO',\n" +
                "        'data_bits': 8,\n" +
                "        'stop_bits': 1\n" +
                "      },\n" +
                "      'devices': {\n" +
                "        'test_device': {\n" +
                "          'driver': 'test_driver',\n" +
                "          'settings': {\n" +
                "            'value': 7\n" +
                "           }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "  }\n" +
                "}\n"
        val config = readConfig(StringReader(configString), ::testCatalogue)

        assertEquals(1, config.buses.size)

        val actualBus = config.buses["foo_bus"]!!
        val expectedDevice = DeviceNode("test_driver", TestDeviceSettings(7))
        val actualDevice = actualBus.devices["test_device"]!!
        assertEquals(expectedDevice, actualDevice)
    }
}