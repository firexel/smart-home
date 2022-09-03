package com.seraph.connector.configuration

import java.time.LocalDateTime

interface ConfigStorage {
    suspend fun list(): List<ConfigHeader>
    suspend fun load(id: ConfigHeader.Id): ConfigBody
    suspend fun save(config: String): ConfigHeader

    data class ConfigHeader(
        val id: Id,
        val dateCreated: LocalDateTime
    ) {
        data class Id(val hash: String) {
            override fun toString(): String = hash
        }
    }

    data class ConfigBody(
        val header: ConfigHeader,
        val content: String
    )
}