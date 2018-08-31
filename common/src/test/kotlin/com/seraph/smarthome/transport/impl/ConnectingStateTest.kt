package com.seraph.smarthome.transport.impl

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.util.State
import org.junit.Test

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal class ConnectingStateTest : BaseStateTest() {

    override fun createState(exchanger: Exchanger<SharedData>): State
            = ConnectingState(exchanger)

    @Test
    fun testConnectedSuccessfully() {
        val client = mock<Client> {
            on { it.connect(any(), any()) } doAnswer {
                val success: () -> Unit = it.getArgument(0)
                success()
            }
        }
        assertStateAfterBegin(client, ConnectedState::class)
    }

    @Test
    fun testFatalConnectionFailure() {
        val client = mock<Client> {
            on { it.connect(any(), any()) } doAnswer {
                val fail: (ClientException) -> Unit = it.getArgument(1)
                fail(ClientException(ClientException.Reason.BAD_CLIENT_CONFIGURATION))
            }
        }
        assertStateAfterBegin(client, DisconnectedState::class)
    }

    @Test
    fun testFatalConnectionDenial() {
        val client = mock<Client> {
            on { it.connect(any(), any()) } doAnswer {
                throw ClientException(ClientException.Reason.BAD_CLIENT_CONFIGURATION)
            }
        }
        assertStateAfterBegin(client, DisconnectedState::class)
    }

    @Test
    fun testNonFatalConnectionFailure() {
        val client = mock<Client> {
            on { it.connect(any(), any()) } doAnswer {
                val fail: (ClientException) -> Unit = it.getArgument(1)
                fail(ClientException(ClientException.Reason.BAD_NETWORK))
            }
        }
        assertStateAfterBegin(client, WaitingState::class)
    }

    @Test
    fun testNonFatalConnectionDenial() {
        val client = mock<Client> {
            on { it.connect(any(), any()) } doAnswer {
                throw ClientException(ClientException.Reason.BAD_NETWORK)
            }
        }
        assertStateAfterBegin(client, WaitingState::class)
    }
}