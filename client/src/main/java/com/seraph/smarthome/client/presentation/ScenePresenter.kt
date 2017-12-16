package com.seraph.smarthome.client.presentation

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
}