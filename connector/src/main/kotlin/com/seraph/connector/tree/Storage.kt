package com.seraph.connector.tree

interface StorageProvider {
    fun makeStorage(key: String): Storage
}

interface Storage {
    suspend fun put(key: String, bytes:ByteArray?)
    suspend fun get(key: String): ByteArray?
}

