package com.seraph.connector.tools

import java.sql.DriverManager

class SqliteStorageProvider {
    fun test() {
        val connection = DriverManager.getConnection("jdbc:sqlite:test.db")
        connection.close()
    }
}