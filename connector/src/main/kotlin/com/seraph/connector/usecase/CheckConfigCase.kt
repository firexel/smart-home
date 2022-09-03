package com.seraph.connector.usecase

import com.seraph.connector.configuration.ConfigChecker
import com.seraph.connector.configuration.ConfigExecutor
import com.seraph.connector.configuration.ExecutionNote

class CheckConfigCase(
    private val config: String,
    private val checker: ConfigChecker
) {
    suspend fun run(): List<ExecutionNote> {
        val errors = ConfigExecutor(config).execute(checker)
        return errors + checker.results
    }
}