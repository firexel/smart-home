package com.seraph.smarthome.logic.devices

import com.nhaarman.mockito_kotlin.*
import com.seraph.smarthome.logic.VirtualDevice
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by aleksandr.naumov on 28.12.17.
 */
class VirtualSwitchTest {

    private lateinit var indicator: VirtualDevice.Updatable<Boolean>
    private lateinit var output: VirtualDevice.Updatable<Boolean>
    private lateinit var toggle: VirtualDevice.Observable<Unit>
    private lateinit var visitor: VirtualDevice.Visitor

    @Before
    fun setup() {
        indicator = mock()
        output = mock()
        toggle = mock()
        visitor = mock {
            on { declareIndicator(any(), any()) } doReturn indicator
            on { declareBoolOutput(any(), any()) } doReturn output
            on { declareAction(any(), any()) } doReturn toggle
        }
        VirtualSwitch().configure(visitor)
    }

    @Test
    fun testDeclaredAllTheNecessaryEndpoints() {
        verify(visitor).declareAction(any(), eq(VirtualDevice.Purpose.MAIN))
        verify(visitor).declareBoolOutput(any(), any())
        verify(visitor).declareIndicator(any(), eq(VirtualDevice.Purpose.STATE))
    }

    @Test
    fun testSwitchChangesIndicatorWhenToggled() {
        val toggle = extractToggleFunction()

        assertIndicatorStateChange(false)
        assertOutputStateChange(false)

        toggle(Unit)

        assertIndicatorStateChange(true)
        assertOutputStateChange(true)

        toggle(Unit)

        assertIndicatorStateChange(false)
        assertOutputStateChange(false)
    }

    private fun extractToggleFunction(): (Unit) -> Unit {
        var toggleFunction: (Unit) -> Unit = {}
        argumentCaptor<(Unit) -> Unit>().apply {
            verify(toggle, times(1)).observe(capture())
            toggleFunction = firstValue
        }
        return toggleFunction
    }

    private fun assertOutputStateChange(expected: Boolean) {
        argumentCaptor<() -> Boolean>().apply {
            verify(output, times(1)).use(capture())
            Assert.assertEquals(expected, firstValue())
        }
    }

    private fun assertIndicatorStateChange(expected: Boolean) {
        argumentCaptor<() -> Boolean>().apply {
            verify(indicator, times(1)).use(capture())
            Assert.assertEquals(expected, firstValue())
        }
    }
}