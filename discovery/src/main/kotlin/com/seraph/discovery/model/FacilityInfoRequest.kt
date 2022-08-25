package com.seraph.discovery.model

import kotlinx.serialization.Serializable

@Serializable
data class FacilityInfoRequest(
    val port: Int
)