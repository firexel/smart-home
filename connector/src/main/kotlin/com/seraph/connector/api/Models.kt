package com.seraph.connector.api

@kotlinx.serialization.Serializable
data class ConfigInstallResponse(
    val status: InstallStatus,
    val errors: List<String>,
    val warnings: List<String>
)

@kotlinx.serialization.Serializable
data class ConfigCheckResponse(
    val errors: List<String>,
    val warnings: List<String>
)

@kotlinx.serialization.Serializable
enum class InstallStatus { OK, NOT_INSTALLED }