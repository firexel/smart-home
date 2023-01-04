package com.seraph.connector.storage

import com.seraph.connector.tree.Storage
import com.seraph.connector.tree.StorageProvider
import com.seraph.smarthome.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.support.sqlite.insertOrUpdate
import java.sql.DriverManager
import java.sql.SQLException

@OptIn(ExperimentalCoroutinesApi::class)
class SqliteStorageProvider(
    private val database: Database, // Database.connect ("jdbc:sqlite:test.db")
    private val dispatcher: CoroutineDispatcher,
    private val log: Log
) : StorageProvider {

    companion object {
        fun open(dbQualifier: String, log: Log): SqliteStorageProvider {
            val dispatcher = Dispatchers.IO.limitedParallelism(1)
            createTables(dbQualifier)
            return SqliteStorageProvider(Database.connect(dbQualifier), dispatcher, log)
        }

        private fun createTables(dbQualifier: String) {
            DriverManager.getConnection(dbQualifier).use { conn ->
                try {
                    conn.createStatement().use { stmt ->
                        stmt.execute(MODEL_SCHEMA_STATEMENT)
                    }
                } catch (e: SQLException) {
                    throw IllegalStateException("Cannot init DB in $dbQualifier", e)
                }
            }
        }
    }

    override fun makeStorage(key: String): Storage = StorageWrapper(key)

    inner class StorageWrapper(
        private val owner: String
    ) : Storage {

        override suspend fun put(key: String, bytes: ByteArray?) = withContext(dispatcher) {
            if (bytes != null) {
                database.insertOrUpdate(Values) {
                    set(Values.owner, owner)
                    set(Values.field, key)
                    set(Values.data, bytes)
                    set(Values.type, Types.BINARY)
                    onConflict {
                        set(Values.data, excluded(Values.data))
                        set(Values.type, excluded(Values.type))
                    }
                }
                log.v("$owner:$key ${bytes.size} stored")
            } else {
                database.delete(Values) { (Values.owner eq owner) and (Values.field eq key) }
                log.v("$owner:$key deleted")
            }
        }

        override suspend fun get(key: String): ByteArray? = withContext(dispatcher) {
            val iterator = database.from(Values)
                .select(Values.data, Values.type)
                .where { (Values.owner eq owner) and (Values.field eq key) }
                .limit(1)
                .iterator()

            if (!iterator.hasNext()) {
                null
            } else {
                val result = iterator.next()
                val data = result[Values.data]
                val type = result[Values.type]
                if (type != Types.BINARY) {
                    log.w("Unknown type of $type of $owner:$key. Removing data")
                    database.delete(Values) { (Values.owner eq owner) and (Values.field eq key) }
                    null
                } else {
                    data
                }
            }
        }
    }
}