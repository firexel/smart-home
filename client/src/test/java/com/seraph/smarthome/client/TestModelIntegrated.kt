package com.seraph.smarthome.client

import com.seraph.smarthome.model.Block
import com.seraph.smarthome.model.Endpoint
import com.seraph.smarthome.model.Network
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TestModelIntegrated {
    @Test
    fun modelIsIntegrated() {
        assertEquals(newNetwork(), newNetwork())
    }

    private fun newNetwork() = Network(listOf(
            Block(
                    Block.Id("id1"),
                    "block1",
                    listOf(Endpoint(
                            Endpoint.Id("endpoint1"),
                            "endpoint1",
                            false,
                            Endpoint.Type.INTEGER
                    )),
                    emptyList()
            )),
            emptyList()
    )
}
