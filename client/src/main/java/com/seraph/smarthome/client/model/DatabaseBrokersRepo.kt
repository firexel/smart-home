package com.seraph.smarthome.client.model

import android.arch.persistence.room.*
import android.content.Context
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by alex on 09.12.17.
 */

class DatabaseBrokersRepo(context: Context) : BrokersSettingsRepo {

    private val database = Room.databaseBuilder<BrokersDb>(context, BrokersDb::class.java, "brokers").build()
    private val brokersTable = database.brokersTable()

    override fun getBrokersSettings(): Observable<List<BrokerSettings>> {
        return Observable.fromCallable { brokersTable.selectAllBrokers() }
                .subscribeOn(Schedulers.io())
                .map { it.map(::fromStoredBroker) }
    }

    override fun saveBrokerSettings(brokerSettings: BrokerSettings): Observable<Unit> {
        return Observable.fromCallable { brokersTable.insertBroker(toStoredBroker(brokerSettings)) }
                .subscribeOn(Schedulers.io())
    }

    override fun findBrokerSettings(id: Int): Observable<BrokerSettings?> {
        return Observable.fromCallable { brokersTable.findBrokerWithId(id) }
                .subscribeOn(Schedulers.io())
                .map { it.run(::fromStoredBroker) }
    }
}

private fun toStoredBroker(model: BrokerSettings)
        = StoredBrokerSettings(model.id, model.host, model.port)

private fun fromStoredBroker(stored: StoredBrokerSettings)
        = BrokerSettings(stored.id, stored.host, stored.port)

@Entity(tableName = "brokers")
data class StoredBrokerSettings(
        @PrimaryKey(autoGenerate = true) @ColumnInfo var id: Int,
        @ColumnInfo var host: String,
        @ColumnInfo var port: Int)

@Dao
interface BrokersTable {
    @Insert
    fun insertBroker(brokerSettings: StoredBrokerSettings)

    @Query("SELECT * from brokers")
    fun selectAllBrokers(): List<StoredBrokerSettings>

    @Query("SELECT * from brokers where id = :id")
    fun findBrokerWithId(id: Int): StoredBrokerSettings
}

@Database(entities = [(StoredBrokerSettings::class)], version = 1)
abstract class BrokersDb : RoomDatabase() {
    abstract fun brokersTable(): BrokersTable
}