package com.seraph.connector.tree

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlin.reflect.KClass

@OptIn(DelicateCoroutinesApi::class)
class TreeHolder {

    private val nodeCache = mutableMapOf<Key, Node>()
    private val context = newFixedThreadPoolContext(1, "Connector tree dispatcher")
    private val scope = CoroutineScope(context)
    private val consumerJunctions = mutableMapOf<Node.Consumer<*>, ConsumerJunction<*>>()
    private val producerJunctions = mutableMapOf<Node.Producer<*>, ProducerJunction<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Node> obtain(token: Any, clazz: KClass<T>, factory: () -> T): T {
        val key = Key(token, clazz)
        var node = nodeCache[key]
        if (node == null) {
            node = factory()
            nodeCache[key] = node
            install(node)
        }
        return node as T
    }

    fun install(node: Node) {
        scope.launch {
            node.run(this)
        }
    }

    fun <T> connect(producer: Node.Producer<T>, consumer: Node.Consumer<T>) {
        scope.launch {
            val inbound = consumer.junction()
            inbound.attachedProducers().forEach { it.detach(inbound) }
            producer.junction().attach(inbound)
        }
    }

    fun <T> disconnect(consumer: Node.Consumer<T>) {
        scope.launch {
            val inbound = consumer.junction()
            inbound.attachedProducers().forEach { it.detach(inbound) }
        }
    }

    fun stop() {
        scope.cancel()
        nodeCache.clear()
        consumerJunctions.clear()
        producerJunctions.clear()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Node.Producer<T>.junction(): ProducerJunction<T> {
        return producerJunctions.getOrPut(this) {
            ProducerJunction(this)
        } as ProducerJunction<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Node.Consumer<T>.junction(): ConsumerJunction<T> {
        return consumerJunctions.getOrPut(this) {
            ConsumerJunction(this)
        } as ConsumerJunction<T>
    }

    private fun <T> ConsumerJunction<T>.attachedProducers(): List<ProducerJunction<*>> {
        return producerJunctions.values.filter { it.isAttached(this) }
    }

    data class Key(
        val token: Any,
        val type: KClass<*>
    )

    inner class ConsumerJunction<T>(
        private val consumer: Node.Consumer<T>
    ) {
        val stateFlow = MutableStateFlow<T?>(null)

        init {
            scope.launch {
                consumer.consume(stateFlow)
            }
        }
    }

    inner class ProducerJunction<T>(
        private val producer: Node.Producer<T>
    ) {
        private val consumers = mutableSetOf<ConsumerJunction<T>>()
        private var lastValue: T? = null

        init {
            scope.launch {
                producer.flow.stateIn(scope).filterNotNull().collect { value ->
                    lastValue = value
                    consumers.forEach { it.stateFlow.value = value }
                }
            }
        }

        fun attach(junction: ConsumerJunction<T>) {
            scope.launch {
                consumers.add(junction)
                if (lastValue != null) {
                    junction.stateFlow.value = lastValue!!
                }
            }
        }

        fun detach(junction: ConsumerJunction<*>) {
            scope.launch {
                consumers.remove(junction)
            }
        }

        fun isAttached(junction: ConsumerJunction<*>): Boolean {
            return consumers.contains(junction)
        }
    }
}