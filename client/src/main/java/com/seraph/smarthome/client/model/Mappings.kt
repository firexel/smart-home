package com.seraph.smarthome.client.model

import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.transport.Broker

/**
 * Created by aleksandr.naumov on 30.12.17.
 */

fun Broker.BrokerState.map(): BrokerConnection.State = this.accept(MapVisitor())

class MapVisitor : Broker.BrokerState.Visitor<BrokerConnection.State> {
    override fun onConnectedState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onConnectedState()
        }
    }

    override fun onDisconnectedState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onDisconnectedState()
        }
    }

    override fun onDisconnectingState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onDisconnectingState()
        }
    }

    override fun onWaitingState(msToReconnect: Long): BrokerConnection.State = object : BrokerConnection.State {
        private val creationTime = System.currentTimeMillis()

        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onWaitingState(
                    (creationTime + msToReconnect) - System.currentTimeMillis()
            )
        }
    }

    override fun onConnectingState(): BrokerConnection.State = object : BrokerConnection.State {
        override fun <T> accept(visitor: BrokerConnection.State.Visitor<T>): T {
            return visitor.onConnectingState()
        }
    }
}
