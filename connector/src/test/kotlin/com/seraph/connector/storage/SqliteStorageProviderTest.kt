package com.seraph.connector.storage

import com.seraph.smarthome.util.NoLog
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

const val DB_NAME: String = "test.db"

internal class SqliteStorageProviderTest {

    @BeforeEach
    @AfterEach
    fun deleteDatabase() {
        File(DB_NAME).delete()
    }

    private fun makeTest(test: suspend (provider: SqliteStorageProvider) -> Unit) {
        runBlocking {
            test(SqliteStorageProvider.open("jdbc:sqlite:${DB_NAME}", NoLog()))
        }
    }

    @Test
    fun testSubsequentPutsRewritesPrevious() = makeTest { provider ->
        val storage = provider.makeStorage("test")
        storage.put("value", "test".toByteArray())
        storage.put("value", "test2".toByteArray())
        assertEquals("test2", String(storage.get("value")!!))
    }

    @Test
    fun testSubsequentPutOfNullDeletesRow() = makeTest { provider ->
        val storage = provider.makeStorage("test")
        storage.put("value", "test".toByteArray())
        storage.put("value", null)
        assertNull(storage.get("value"))
    }

    @Test
    fun testSameKeysDifferentOwners() = makeTest { provider ->
        val storage1 = provider.makeStorage("owner1")
        storage1.put("value", "owner1value".toByteArray())

        val storage2 = provider.makeStorage("owner2")
        storage2.put("value", "owner2value".toByteArray())

        assertEquals("owner1value", String(storage1.get("value")!!))
        assertEquals("owner2value", String(storage2.get("value")!!))
    }

    @Test
    fun testDifferentKeysSameOwner() = makeTest { provider ->
        val storage1 = provider.makeStorage("owner1")
        storage1.put("value", "owner1value".toByteArray())
        storage1.put("value2", "owner1value2".toByteArray())

        assertEquals("owner1value", String(storage1.get("value")!!))
        assertEquals("owner1value2", String(storage1.get("value2")!!))
    }
}