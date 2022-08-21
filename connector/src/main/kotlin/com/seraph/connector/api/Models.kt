package com.seraph.connector.api

import kotlinx.serialization.Serializable

@Serializable
data class ConfigApplyResponse(
    val status: ApplyStatus,
    val errors: List<String>,
    val warnings: List<String>,
    val config: ConfigDataResponse?
)

@Serializable
data class ConfigsListResponse(
    val configs: List<ConfigDataResponse>
)

@Serializable
data class ConfigDataResponse(
    val id: String,
    val dateCreated: String
)

@Serializable
data class ConfigCheckResponse(
    val errors: List<String>,
    val warnings: List<String>
)

@Serializable
enum class ApplyStatus { OK, NOT_APPLIED }

@Serializable
data class EndpointDumpResponse(
    val type: String,
    val direction: String,
    val value: String?
)