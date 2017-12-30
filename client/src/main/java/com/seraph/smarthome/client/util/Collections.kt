package com.seraph.smarthome.client.util

/**
 * Created by aleksandr.naumov on 30.12.17.
 */
inline fun <T> List<T>.copy(mutatorBlock: MutableList<T>.() -> Unit): List<T> {
    return toMutableList().apply(mutatorBlock)
}

inline fun <T> List<T>.replace(predicate: (T) -> Boolean, constructor: (T) -> T) = copy {
    val index = indexOfFirst(predicate)
    if (index >= 0) {
        this[index] = constructor(this[index])
    }
}