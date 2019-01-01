package com.seraph.smarthome.transport.impl

import com.nhaarman.mockito_kotlin.*
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class LocalBrokerTest {

    @Test
    fun testPublishPropagation() {
        val externalBroker: Broker = mock()
        val testBroker = LocalBroker(externalBroker)

        verify(externalBroker, never()).publish(any(), any())

        testBroker.publish(Topic("test"), ByteArray(1))

        verify(externalBroker, times(1)).publish(any(), any())
        verify(externalBroker, times(1)).publish(eq(Topic("test")), eq(ByteArray(1)))
    }

    @Test
    fun testSubscribePropagationAtFirstCall() {
        val externalBroker: Broker = mock()
        val testBroker = LocalBroker(externalBroker)

        verify(externalBroker, never()).subscribe(any(), any())

        val listener: (Topic, ByteArray) -> Unit = { _, _ -> Unit }
        testBroker.subscribe(Topic("test"), listener)

        verify(externalBroker, times(1)).subscribe(any(), any())
        verify(externalBroker, times(1)).subscribe(eq(Topic("test")), any())
    }

    @Test
    fun testSingleSubscribeAtExternal() {
        val externalBroker: Broker = mock()
        val testBroker = LocalBroker(externalBroker)

        verify(externalBroker, never()).subscribe(any(), any())

        testBroker.subscribe(Topic("test")) { _, _ -> Unit }
        testBroker.subscribe(Topic("test")) { _, _ -> Unit }

        verify(externalBroker, times(1)).subscribe(any(), any())
        verify(externalBroker, times(1)).subscribe(eq(Topic("test")), any())
    }

    @Test
    fun testAllRequiredListenersFiredAtMatchedSubscribe() {
        val externalBroker: Broker = mock()
        val testBroker = LocalBroker(externalBroker)
        val listener1Fired = AtomicBoolean()
        val listener2Fired = AtomicBoolean()
        val listener3Fired = AtomicBoolean()

        testBroker.subscribe(Topic("test")) { _, _ -> listener1Fired.set(true) }
        testBroker.subscribe(Topic("test")) { _, _ -> listener2Fired.set(true) }
        testBroker.subscribe(Topic("other")) { _, _ -> listener3Fired.set(true) }

        Assert.assertFalse(listener1Fired.get() || listener2Fired.get() || listener3Fired.get())
        val captor = argumentCaptor<(Topic, ByteArray) -> Unit>()
        verify(externalBroker, times(1)).subscribe(eq(Topic("test")), captor.capture())

        captor.firstValue(Topic("test"), ByteArray(1))

        Assert.assertTrue(listener1Fired.get())
        Assert.assertTrue(listener2Fired.get())
        Assert.assertFalse(listener3Fired.get())
    }
}