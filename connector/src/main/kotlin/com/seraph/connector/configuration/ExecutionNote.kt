package com.seraph.connector.configuration

data class ExecutionNote(
    val severity: Severity,
    val message: String
) {
    enum class Severity {
        ERROR, WARNING
    }
}

fun List<ExecutionNote>.errors() = filter { it.severity == ExecutionNote.Severity.ERROR }
fun List<ExecutionNote>.warns() = filter { it.severity == ExecutionNote.Severity.WARNING }