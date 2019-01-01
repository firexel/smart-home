package com.seraph.smarthome.transport.impl

import com.nhaarman.mockito_kotlin.*
import com.seraph.smarthome.util.Exchanger
import com.seraph.smarthome.util.State
import org.junit.Assert
import org.junit.Test

/**
 * Created by aleksandr.naumov on 03.01.18.
 */
internal class ExchangerTest {

    @Test
    fun testBeginEngagesFirstState() {
        val exchanger = Exchanger<State, MockData>()
        val state1: State = mock()
        val data = MockData(state1)
        verify(state1, never()).engage()

        exchanger.begin(data)

        verify(state1, times(1)).engage()
        Assert.assertSame(state1, exchanger.sync { it.state })
    }

    @Test
    fun testStateChange() {
        val exchanger = Exchanger<State, MockData>()
        val state1: State = mock()
        val state2: State = mock()
        val data = MockData(state1)
        exchanger.begin(data)
        verify(state1, never()).disengage()
        verify(state2, never()).engage()

        exchanger.transact {
            data.copy(state = state2)
        }

        verify(state1, times(1)).disengage()
        verify(state2, times(1)).engage()
        Assert.assertSame(state2, exchanger.sync { it.state })
    }

    @Test
    fun testStateChangeDuringBegin() {
        val exchanger = Exchanger<State, MockData>()
        val state2: State = mock()
        val state1: State = mock {
            on { it.engage() } doAnswer { exchanger.transact { it.copy(state = state2) } }
        }
        val data = MockData(state1)
        verify(state1, never()).engage()

        exchanger.begin(data)
        with(inOrder(state1, state2)) {
            verify(state1, times(1)).engage()
            verify(state1, times(1)).disengage()
            verify(state2, times(1)).engage()
        }
        verify(state2, never()).disengage()
        Assert.assertSame(state2, exchanger.sync { it.state })
    }

    @Test
    fun testStateChangeDuringBeginAndEngage() {
        val exchanger = Exchanger<State, MockData>()
        val state3: State = mock()
        val state2: State = mock {
            on { it.engage() } doAnswer { exchanger.transact { it.copy(state = state3) } }
        }
        val state1: State = mock {
            on { it.engage() } doAnswer { exchanger.transact { it.copy(state = state2) } }
        }
        val data = MockData(state1)
        verify(state1, never()).engage()

        exchanger.begin(data)
        with(inOrder(state1, state2, state3)) {
            verify(state1, times(1)).engage()
            verify(state1, times(1)).disengage()
            verify(state2, times(1)).engage()
            verify(state2, times(1)).disengage()
            verify(state3, times(1)).engage()
        }
        verify(state3, never()).disengage()
        Assert.assertSame(state3, exchanger.sync { it.state })
    }

    @Test
    fun testStateChangeDuringNestedTransacts() {
        val exchanger = Exchanger<State, MockData>()
        val state2: State = mock()
        val state1: State = mock {
            on { it.engage() } doAnswer {
                exchanger.transact {
                    exchanger.transact { it.copy(state = state2) }
                    it
                }
            }
        }
        val data = MockData(state1)
        verify(state1, never()).engage()

        exchanger.begin(data)
        with(inOrder(state1, state2)) {
            verify(state1, times(1)).engage()
            verify(state1, times(1)).disengage()
            verify(state2, times(1)).engage()
        }
        verify(state2, never()).disengage()
        Assert.assertSame(state2, exchanger.sync { it.state })
    }

    private data class MockData(
            override val state: State
    ) : Exchanger.StateData<State>
}