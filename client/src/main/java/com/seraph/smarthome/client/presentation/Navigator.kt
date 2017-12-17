package com.seraph.smarthome.client.presentation

interface Navigator {
    fun show(screen: Screen)
    fun goBack()
    fun <T> getCurrentScreen(): T
}