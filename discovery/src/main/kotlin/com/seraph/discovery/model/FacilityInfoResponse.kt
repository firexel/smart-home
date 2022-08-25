package com.seraph.discovery.model

import kotlinx.serialization.Serializable

@Serializable
data class FacilityInfoResponse(
    val id: String,
    val name: String,
    val imageUrl: String,
    val brokerHost: String,
    val brokerPort: Int,
    val brokerLogin: String?,
    val brokerPassword: String?
)