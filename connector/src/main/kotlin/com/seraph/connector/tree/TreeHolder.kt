package com.seraph.connector.tree

import kotlin.reflect.KClass

class TreeHolder {

    private val nodeCache = mutableMapOf<Key, Node>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Node> obtain(token: Any, clazz: KClass<T>, factory: () -> T): T {
        val key = Key(token, clazz)
        var node = nodeCache[key]
        if (node == null) {
            node = factory()
            nodeCache[key] = node
        }
        return node as T
    }

    fun install(node: Node) {
        TODO()
    }

    fun <T> connect(producer: Node.Producer<T>, consumer: Node.Consumer<T>) {
        TODO("not implemented")
    }

    fun <T> disconnect(consumer: Node.Consumer<T>) {
        TODO("not implemented")
    }

    data class Key(
        val token: Any,
        val type: KClass<*>
    )
}