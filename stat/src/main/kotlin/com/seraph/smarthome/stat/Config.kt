package com.seraph.smarthome.stat

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader

@Serializable
sealed class Output

@Serializable
data class GraphiteOutput(
        val address: String,
        val repeatDelaySeconds: Int,
        val configFolderPath: String? = null
) : Output()

@Serializable
data class ClickHouseOutput(
        val address: String,
        val tableName: String
) : Output()

@Serializable
data class Config(
        val network: String,
        val outputs: List<Output>
)

fun readConfig(file: File): Config {
    return Json.decodeFromString(FileReader(file).readText())
}