package com.seraph.connector.tools

import org.junit.jupiter.api.Test

internal class SqliteStorageProviderTest {
    @Test
    fun testConnectionOpens() {
        SqliteStorageProvider().test()
    }
}