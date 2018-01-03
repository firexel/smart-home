package com.seraph.smarthome.transport.impl

interface StateData {
    val state: State
}

data class SharedData(
        val client: Client,
        val actions: List<(Client) -> Unit>,
        override val state: State,
        val timesRetried: Int
) : StateData