package com.seraph.smarthome.transport.impl

import com.nhaarman.mockito_kotlin.mock
import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.util.State
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal open class BaseStateTest {

    protected lateinit var exchanger: Exchanger<SharedData>
    protected lateinit var state: State

    @Before
    fun setup() {
        exchanger = Exchanger()
        state = createState(exchanger)
    }

    protected open fun createState(exchanger: Exchanger<SharedData>): State
            = MockBaseState(exchanger)

    @Test
    fun testTransactionCalledOnCurrentState() {
        val exchanger = Exchanger<SharedData>()
        val state = MockBaseState(exchanger)
        exchanger.begin(SharedData(
                mock(),
                emptyList(),
                state,
                0
        ))
        state.assertTransactionCalls()
    }

    @Test
    fun testTransactionDoesNotCalledOnNonCurrentState() {
        val exchanger = Exchanger<SharedData>()
        val state1 = MockBaseState(exchanger)
        exchanger.begin(SharedData(
                mock(),
                emptyList(),
                state1,
                0
        ))
        state1.assertTransactionCalls()
        MockBaseState(exchanger).assertTransactionDoesNotCalls()
    }


    protected fun assertStateAfterBegin(client: Client, clazz: KClass<*>) {
        exchanger.begin(SharedData(client, emptyList(), state, 0))
        assertCurrentState(clazz)
    }

    protected fun assertCurrentState(clazz: KClass<*>) {
        Assert.assertEquals(clazz, exchanger.sync { it.state }::class)
    }

    protected fun assertPendingActions(expectedActions: List<SharedData.Action>) {
        val expected = expectedActions.joinToString()
        val actualActions = exchanger.sync { it.actions }
        val actual = actualActions.joinToString()
        Assert.assertEquals(expected, actual)
        actualActions.indices.forEach {
            Assert.assertSame(expectedActions[it].lambda, actualActions[it].lambda)
        }
    }

    private class MockBaseState(exchanger: Exchanger<SharedData>) : BaseState(exchanger) {
        override fun engage() = Unit
        override fun disengage() = Unit
        override fun <T> accept(visitor: Broker.BrokerState.Visitor<T>): T = throw IllegalStateException("Mock")
        override fun execute(key: Any?, action: (Client) -> Unit) = Unit

        fun assertTransactionCalls() {
            Assert.assertTrue(checkTransactionCall())
        }

        fun assertTransactionDoesNotCalls() {
            Assert.assertFalse(checkTransactionCall())
        }

        private fun checkTransactionCall(): Boolean {
            var transactionCalled = false
            transact {
                transactionCalled = true
                it
            }
            return transactionCalled
        }
    }
}