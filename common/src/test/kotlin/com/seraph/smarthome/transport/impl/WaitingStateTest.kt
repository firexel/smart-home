package com.seraph.smarthome.transport.impl

import com.nhaarman.mockito_kotlin.mock
import com.seraph.smarthome.transport.Broker
import org.junit.Assert
import org.junit.Test

/**
 * Created by aleksandr.naumov on 04.01.18.
 */
internal class WaitingStateTest : BaseStateTest() {
    override fun createState(exchanger: Exchanger<SharedData>): State
            = WaitingState(exchanger, StoppedClock(0))

    @Test
    fun testTimeToWaitInFirstTry() {
        assertReconnectionDelay(0, 1000)
    }

    @Test
    fun testTimeToWaitInSecondTry() {
        assertReconnectionDelay(1, 2000)
    }

    @Test
    fun testTimeToWaitInThirdTry() {
        assertReconnectionDelay(2, 4000)
    }

    @Test
    fun testTimeToWaitInSixTry() {
        assertReconnectionDelay(5, 32000)
    }

    @Test
    fun testTimeToWaitInTensTry() {
        assertReconnectionDelay(9, 60000)
    }

    @Test
    fun testTimeToWaitAfterYearOfDisconnection() {
        assertReconnectionDelay(365 * 24 * 60, 60000)
    }

    private fun assertReconnectionDelay(timesRetried: Int, expectedDelay: Long) {
        exchanger.begin(SharedData(mock(), emptyList(), state, timesRetried))
        val timeToWait = state.accept(TimeVisitor())
        Assert.assertEquals(expectedDelay, timeToWait)
    }

    class TimeVisitor : Broker.BrokerState.Visitor<Long> {
        override fun onConnectedState(): Long = throw IllegalStateException()
        override fun onDisconnectedState(): Long = throw IllegalStateException()
        override fun onDisconnectingState(): Long = throw IllegalStateException()
        override fun onConnectingState(): Long = throw IllegalStateException()

        override fun onWaitingState(msToReconnect: Long): Long = msToReconnect
    }

    class StoppedClock(override val time: Long) : WaitingState.Clock
}