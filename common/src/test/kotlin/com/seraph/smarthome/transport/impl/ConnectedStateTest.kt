package com.seraph.smarthome.transport.impl

import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal class ConnectedStateTest : BaseStateTest() {
    override fun createState(exchanger: Exchanger<SharedData>): State
            = ConnectedState(exchanger)

    @Test
    fun testFatalExceptionDuringClientAction() {
        val client: Client = mock()
        exchanger.begin(SharedData(client, emptyList(), state, 0))

        state.execute { throw ClientException(ClientException.Reason.BAD_CLIENT_STATE) }

        assertCurrentState(DisconnectingState::class)
        assertPendingActions(emptyList())
    }

    @Test
    fun testNonFatalExceptionDuringClientAction() {
        val client: Client = mock()
        exchanger.begin(SharedData(client, emptyList(), state, 0))
        val action: (Client) -> Unit = {
            throw ClientException(ClientException.Reason.BAD_NETWORK)
        }

        state.execute(action)

        assertCurrentState(WaitingState::class)
        assertPendingActions(listOf(action))
    }

    @Test
    fun testDisconnectedCallbackFiredWithFatalError() {
        val client: Client = object : Client by mock() {
            override var disconnectionCallback: ((ClientException) -> Unit)? = null
        }
        exchanger.begin(SharedData(client, emptyList(), ConnectedState(exchanger), 0))

        Assert.assertNotNull(client.disconnectionCallback)
        client.disconnectionCallback?.invoke(ClientException(ClientException.Reason.BAD_CLIENT_STATE))
        assertCurrentState(DisconnectingState::class)
    }


    @Test
    fun testDisconnectedCallbackFiredWithNonFatalError() {
        val client: Client = object : Client by mock() {
            override var disconnectionCallback: ((ClientException) -> Unit)? = null
        }
        exchanger.begin(SharedData(client, emptyList(), ConnectedState(exchanger), 0))

        Assert.assertNotNull(client.disconnectionCallback)
        client.disconnectionCallback?.invoke(ClientException(ClientException.Reason.BAD_NETWORK))
        assertCurrentState(WaitingState::class)
    }

    @Test
    fun testPendingActionsGetExecutedOnceStateEngaged() {
        val executionLog = mutableListOf<Int>()
        val actions: List<(Client) -> Unit> = (0 until 5).map {
            { _: Client ->
                executionLog.add(it)
                Unit
            }
        }

        exchanger.begin(SharedData(mock(), actions, ConnectedState(exchanger), 0))

        assertPendingActions(emptyList())
        Assert.assertEquals((0 until 5).joinToString(), executionLog.joinToString())
    }

    @Test
    fun testActionListPartiallyExecutedDuringEngage() {
        val executionLog = mutableListOf<Int>()
        val actions: List<(Client) -> Unit> = (0 until 5).map {
            { _: Client ->
                if (it == 2) {
                    throw ClientException(ClientException.Reason.BAD_NETWORK)
                } else {
                    executionLog.add(it)
                }
                Unit
            }
        }

        exchanger.begin(SharedData(mock(), actions, ConnectedState(exchanger), 0))

        assertPendingActions(listOf(2, 3, 4).map { actions[it] })
        Assert.assertEquals(listOf(0, 1).joinToString(), executionLog.joinToString())
    }
}