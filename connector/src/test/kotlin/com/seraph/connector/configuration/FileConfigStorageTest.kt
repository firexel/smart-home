package com.seraph.connector.configuration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class FileConfigStorageTest {

    private val testConfig = """
        |test line 1
        |test line 2
    """.trimMargin()

    @Test
    fun testConfigSavedAndLoaded() = runTest { dir ->
        val storage = FileConfigStorage(dir)
        val savedHeader = storage.save(testConfig)
        val loadedHeader = storage.load(savedHeader.id)
        assertEquals(testConfig, loadedHeader.content)
        assertEquals(savedHeader, loadedHeader.header)
        assertEquals(
            LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            savedHeader.dateCreated.truncatedTo(ChronoUnit.SECONDS)
        )
    }

    @Test
    fun testConfigsList() = runTest { dir ->
        val storage = FileConfigStorage(dir)
        val t1 = storage.save(testConfig)
        val t2 = storage.save(testConfig)
        val configs = storage.list().sortedBy { it.dateCreated }
        assertEquals(2, configs.size)
        assertEquals(t1, configs[0])
        assertEquals(t2, configs[1])
        configs.forEach {
            assertEquals(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                it.dateCreated.truncatedTo(ChronoUnit.SECONDS)
            )
        }
    }

    fun runTest(block: suspend (File) -> Unit) {
        val tmpPath = Files.createTempDirectory(
            Paths.get("").toAbsolutePath(),
            "fileConfigStorageTest" + System.currentTimeMillis()
        )
        val storageDir = tmpPath.toFile()
        storageDir.deleteOnExit()

        try {
            runBlocking {
                block(storageDir)
            }
        } finally {
            storageDir.deleteRecursively()
        }
    }
}