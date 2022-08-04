package com.seraph.connector.configuration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId

class FileConfigStorage(
    private val rootFolder: File
) : ConfigStorage {

    private val configExtension = ".config.kts"

    override suspend fun list(): List<ConfigStorage.ConfigHeader> {
        return withContext(Dispatchers.IO) {
            val files = rootFolder.listFiles { file ->
                file.name.endsWith(configExtension)
            } ?: emptyArray()
            files.map { file -> loadConfigFileAttrsAsData(file) }
        }
    }

    override suspend fun load(id: ConfigStorage.ConfigHeader.Id): ConfigStorage.ConfigBody {
        return withContext(Dispatchers.IO) {
            val file = id.toFile()
            ConfigStorage.ConfigBody(
                loadConfigFileAttrsAsData(file),
                file.readLines(Charsets.UTF_8).joinToString("\n")
            )
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun save(config: String): ConfigStorage.ConfigHeader {
        return withContext(Dispatchers.IO) {
            val id = ConfigStorage.ConfigHeader.Id(md5(config + System.currentTimeMillis()))
            val file = id.toFile()
            file.bufferedWriter(Charsets.UTF_8).use { writer -> writer.append(config) }
            loadConfigFileAttrsAsData(file)
        }
    }

    private fun loadConfigFileAttrsAsData(file: File): ConfigStorage.ConfigHeader {
        val attr: BasicFileAttributes = Files.readAttributes(
            file.toPath(),
            BasicFileAttributes::class.java
        )
        return ConfigStorage.ConfigHeader(
            ConfigStorage.ConfigHeader.Id(file.name.dropLast(configExtension.length)),
            LocalDateTime.ofInstant(
                attr.lastModifiedTime().toInstant(),
                ZoneId.systemDefault()
            )
        )
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(
            1,
            md.digest(input.toByteArray(Charsets.UTF_8))
        ).toString(16)
    }

    private fun ConfigStorage.ConfigHeader.Id.toFile() =
        File(rootFolder, hash + configExtension)
}
