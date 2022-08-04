package com.seraph.connector.api

@kotlinx.serialization.Serializable
data class ConfigApplyResponse(
    val status: ApplyStatus,
    val errors: List<String>,
    val warnings: List<String>,
    val config: ConfigDataResponse?
)

@kotlinx.serialization.Serializable
data class ConfigsListResponse(
    val configs: List<ConfigDataResponse>
)

@kotlinx.serialization.Serializable
data class ConfigDataResponse(
    val id: String,
    val dateCreated: String
)

@kotlinx.serialization.Serializable
data class ConfigCheckResponse(
    val errors: List<String>,
    val warnings: List<String>
)

@kotlinx.serialization.Serializable
enum class ApplyStatus { OK, NOT_APPLIED }