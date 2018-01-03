package com.seraph.smarthome.transport.impl

internal data class SharedData(
        val client: Client,
        val actions: List<(Client) -> Unit>,
        override val state: State,
        val timesRetried: Int
) : Exchanger.StateData