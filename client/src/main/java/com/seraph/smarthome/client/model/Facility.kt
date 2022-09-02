package com.seraph.smarthome.client.model

data class Facility(
    val id: String,
    val name: String,
    val cover: String,
    val brokerHost: String,
    val brokerPort: Int,
    val brokerLogin: String?,
    val brokerPassword: String?
)