package com.seraph.connector.usecase

import com.seraph.connector.configuration.ConfigStorage

/**
 * Returns all stored configs from newest to oldest
 */
class ListConfigsCase(
    private val configStorage: ConfigStorage
) {
    suspend fun run(): List<ConfigStorage.ConfigHeader> {
        return configStorage.list().sortedBy { it.dateCreated }.reversed()
    }
}