package com.seraph.smarthome.transport.impl

internal data class SharedData(
        val client: Client,
        val actions: List<Action> = emptyList(),
        override val state: State,
        val timesRetried: Int
) : Exchanger.StateData {
    data class Action(private val key: Any?, val lambda: (Client) -> Unit) {
        val singleUse = key == null
        val persisted = key != null
    }
}