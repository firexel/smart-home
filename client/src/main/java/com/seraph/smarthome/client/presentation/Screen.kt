package com.seraph.smarthome.client.presentation

import java.io.Serializable

/**
 * Created by aleksandr.naumov on 17.12.17.
 */
interface Screen : Serializable {

    fun <T> acceptVisitor(visitor: Visitor<T>):T

    interface Visitor<T> {
        fun sceneScreenVisited():T
        fun newBrokerScreenVisited():T
        fun brokersListScreenVisited():T
    }
}