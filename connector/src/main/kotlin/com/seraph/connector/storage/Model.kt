package com.seraph.connector.storage

import org.intellij.lang.annotations.Language
import org.ktorm.schema.*

object Values : Table<Nothing>("t_value") {
    val owner = varchar("owner").primaryKey()
    val field = varchar("field").primaryKey()
    val data = bytes("data")
    val type = enum<Types>("type")
}

enum class Types {
    BINARY, FLOAT, INT, BOOLEAN, STRING
}

@Language("SQLITE-SQL")
const val MODEL_SCHEMA_STATEMENT = """
    CREATE TABLE IF NOT EXISTS t_value (
    owner text not null,
    field text not null,
    data blob,
    type text not null,
    PRIMARY KEY (owner, field)
);
"""