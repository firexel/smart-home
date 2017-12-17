package com.seraph.smarthome.client.presentation

import com.seraph.smarthome.client.model.BrokerCredentials

interface ScenePresenter {

    fun onActionPerformed(actionId:String)

    interface View {
        fun onShowActions(actions:Collection<ActionViewModel>)
    }

    data class ActionViewModel (
            val id:String,
            val name:String,
            val value:String
    )

    class SceneScreen(val credentials: BrokerCredentials) : Screen {
        override fun <T> acceptVisitor(visitor: Screen.Visitor<T>): T = visitor.sceneScreenVisited()
    }
}