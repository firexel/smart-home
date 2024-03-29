package com.seraph.smarthome.device

import com.nhaarman.mockito_kotlin.*
import com.seraph.smarthome.domain.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*
import java.util.concurrent.Executor

/**
 * Created by aleksandr.naumov on 08.05.18.
 */
class DriversManagerTest {

    private lateinit var executor: StepByStepExecutor
    private lateinit var network: TestNetwork

    @Before
    fun prepare() {
        executor = StepByStepExecutor()
        network = Mockito.spy(TestNetwork())
    }

    @Test
    fun testInnedDevices() {
        DriversManager(network, Device.Id("root"), executor)
                .addDriver(Device.Id("level1"), TestMultilevelDriver(), executor = ImmediateExecutor())

        executor.fastForward()

        val captor = argumentCaptor<Device>()
        verify(network, times(7)).publish(captor.capture())
        val published = captor.allValues.iterator()

        Assert.assertEquals(Device(Device.Id("root", "level1")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1", "level2_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1", "level2_0", "level3_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1", "level2_0", "level3_1")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1", "level2_1")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1", "level2_1", "level3_0")), published.next())
        Assert.assertEquals(Device(Device.Id("root", "level1", "level2_1", "level3_1")), published.next())
        Assert.assertFalse(published.hasNext())
    }

    @Test
    fun testDeviceOutputsNotUpdatedUntilAllInputsSet() {
        val driver = TestEarlyPoster()

        DriversManager(network, Device.Id("root"), executor)
                .addDriver(Device.Id("early_poster"), driver, executor = ImmediateExecutor())

        executor.fastForward()

        verify(network, times(1)).publish(any<Device>())
        verify(network, never()).set(any(), any<Endpoint<*>>(), any())

        driver.setOutputs()
        executor.fastForward()

        verify(network, never()).set(any(), any<Endpoint<*>>(), any())

        network.emulateDataReceived("root:early_poster", "in1", true)
        executor.fastForward()

        verify(network, never()).set(any(), any<Endpoint<*>>(), any())

        driver.setOutputs()
        executor.fastForward()

        verify(network, never()).set(any(), any<Endpoint<*>>(), any())

        network.emulateDataReceived("root:early_poster", "in2", true)
        executor.fastForward()

        verify(network, never()).set(any(), any<Endpoint<*>>(), any())

        driver.setOutputs()
        executor.fastForward()

        verify(network, times(2)).set(
                eq(Device.Id("root").innerId("early_poster")),
                any<Endpoint<*>>(),
                any()
        )
    }

    class ImmediatePublication : Network.Publication {
        override fun waitForCompletion(millis: Long) {
            // do nothing
        }
    }

    class TestMultilevelDriver : DeviceDriver {
        override fun bind(visitor: DeviceDriver.Visitor) {
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

    class StepByStepExecutor : Executor {

        private val queue: Queue<Runnable> = LinkedList<Runnable>()

        override fun execute(runnable: Runnable?) {
            if (runnable != null) {
                queue.offer(runnable)
            } else {
                throw NullPointerException()
            }
        }

        fun fastForward() {
            while (queue.isNotEmpty()) {
                queue.remove().run()
            }
        }
    }

    open class TestNetwork : Network {

        private val endpointSubscriptions = mutableListOf<EndpointClosure<*>>()

        override fun publish(metainfo: Metainfo): Network.Publication = ImmediatePublication()
        override fun publish(device: Device): Network.Publication = ImmediatePublication()
        override fun <T> set(device: Device.Id, endpoint: Endpoint<T>, data: T): Network.Publication = ImmediatePublication()
        override fun <T> override(device: Device.Id, endpoint: Endpoint<T>, data: T): Network.Publication = ImmediatePublication()

        override fun subscribe(device: Device.Id?, func: (Device) -> Unit): Network.Subscription {
            TODO("not implemented")
        }

        override fun subscribe(func: (Metainfo) -> Unit): Network.Subscription {
            TODO("not implemented")
        }

        override fun <T> subscribe(
                device: Device.Id,
                endpoint: Endpoint<T>,
                func: (Device.Id, Endpoint<T>, T) -> Unit): Network.Subscription {

            endpointSubscriptions.add(EndpointClosure(device, endpoint, func))
            return makeSubscription()
        }

        private fun makeSubscription(): Network.Subscription {
            return object : Network.Subscription {
                override fun unsubscribe() {
                    TODO("not implemented")
                }
            }
        }

        override var statusListener: Network.StatusListener
            get() = TODO("not implemented")
            set(value) = TODO()

        fun <T> emulateDataReceived(device: String, endpoint: String, data: T) {
            val deviceId = Device.Id(device.split(":"))
            val endpointId = Endpoint.Id(endpoint)
            endpointSubscriptions.filter {
                it.deviceId == deviceId && it.endpoint.id == endpointId
            }.apply {
                Assert.assertTrue(isNotEmpty())
            }.forEach {
                val subscription = it as EndpointClosure<T>
                subscription.func(deviceId, subscription.endpoint, data)
            }
        }

        data class EndpointClosure<T>(
                val deviceId: Device.Id,
                val endpoint: Endpoint<T>,
                val func: (Device.Id, Endpoint<T>, T) -> Unit
        )
    }

    class TestEarlyPoster : DeviceDriver {

        private var inputs: List<DeviceDriver.Input<Boolean>> = emptyList()
        private var outputs: List<DeviceDriver.Output<Boolean>> = emptyList()

        override fun bind(visitor: DeviceDriver.Visitor) {
            outputs = listOf("out1", "out2").map {
                visitor.declareOutput(it, Types.BOOLEAN)
            }
            inputs = listOf("in1", "in2").map {
                visitor.declareInput(it, Types.BOOLEAN)
                        .waitForDataBeforeOutput()
            }
            inputs.forEach {
                it.observe {
                    // nothing, just indicate
                }
            }
        }

        fun setOutputs() {
            outputs.forEach { it.set(true) }
        }
    }
}