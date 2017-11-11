package com.seraph.smarthome.client

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TestModelIntegrated {
    @Test
    fun modelIsIntegrated() {
        assertEquals(1, com.seraph.smarthome.model.Test().test())
    }
}
