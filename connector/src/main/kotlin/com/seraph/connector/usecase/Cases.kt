package com.seraph.connector.usecase

interface Cases {
    fun listConfigs(): ListConfigsCase
    fun applyConfig(): ApplyConfigCase
    fun checkConfig(config: String): CheckConfigCase
}