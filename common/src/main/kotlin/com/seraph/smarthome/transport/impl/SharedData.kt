package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.util.Exchanger

internal data class SharedData(
        val client: Client,
        val actions: List<Action> = emptyList(),
        override val state: BaseState,
        val timesRetried: Int
) : Exchanger.StateData<BaseState> {
    data class Action(private val key: Any?, val lambda: (Client) -> Unit) {
        val singleUse = key == null
        val persisted = key != null
    }
}