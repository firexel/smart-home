package com.seraph.smarthome.device

import com.nhaarman.mockito_kotlin.*
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Network
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by aleksandr.naumov on 08.05.18.
 */
class DeviceManagerTest {
    @Test
    fun testInnedDevices() {
        val executor = Executors.newSingleThreadExecutor()
        val network = mock<Network> {
            whenever(it.publish(any<Device>())).thenReturn(ImmediatePublication())
        }

        DeviceManager(network, Device.Id("root"), executor)
                .addDriver(Device.Id("level1"), TestMultilevelDriver(), ImmediateExecutor())

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
        val captor = argumentCaptor<Device>()
        verify(network, times(7)).publish(captor.capture())
        val published = captor.allValues.iterator()

        Assert.assertEquals(Device(Device.Id("root", "level1_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1_0", "level2_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1_0", "level2_0", "level3_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1_0", "level2_0", "level3_1")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1_0", "level2_1")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1_0", "level2_1", "level3_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1_0", "level2_1", "level3_1")), published.next())
        Assert.assertFalse(published.hasNext())
    }

    class ImmediatePublication : Network.Publication {
        override fun waitForCompletion(millis: Long) {
            // do nothing
        }
    }

    class TestMultilevelDriver : DeviceDriver {
        override fun configure(visitor: DeviceDriver.Visitor) {
            (0..1).forEach {
                val level2Visitor = visitor.declareInnerDevice("level2_$it")
                (0..1).forEach {
                    level2Visitor.declareInnerDevice("level3_$it")
                }
            }
        }
    }

    class ImmediateExecutor : Executor {
        override fun execute(p0: Runnable?) {
            p0?.run()
        }
    }
}