package com.seraph.smarthome.client.model

import android.arch.persistence.room.*
import android.content.Context
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import com.seraph.smarthome.model.Metadata

/**
 * Created by alex on 09.12.17.
 */

class DatabaseBrokersRepo(context: Context) : BrokersInfoRepo {

    private val database = Room.databaseBuilder<BrokersDb>(context, BrokersDb::class.java, "brokers").build()
    private val brokersTable = database.brokersTable()

    override fun getBrokersSettings(): Observable<List<BrokerInfo>> {
        return Observable.fromCallable { brokersTable.selectAllBrokers() }
                .subscribeOn(Schedulers.io())
                .map { it.map(::fromStoredBroker) }
    }

    override fun saveBrokerSettings(brokerSettings: BrokerInfo): Observable<Unit> {
        return Observable.fromCallable { brokersTable.insertBroker(toStoredBroker(brokerSettings)) }
                .subscribeOn(Schedulers.io())
    }
}

private fun toStoredBroker(model: BrokerInfo)
        = StoredBrokerSettings(model.credentials.host, model.metadata.name, model.credentials.port)

private fun fromStoredBroker(stored: StoredBrokerSettings) = BrokerInfo(
        Metadata(stored.name),
        BrokerCredentials(stored.host, stored.port)
)

@Entity(tableName = "brokers")
data class StoredBrokerSettings(
        @PrimaryKey(autoGenerate = false) @ColumnInfo var host: String,
        @ColumnInfo var name: String,
        @ColumnInfo var port: Int)

@Dao
interface BrokersTable {
    @Insert
    fun insertBroker(brokerSettings: StoredBrokerSettings)

    @Query("SELECT * from brokers")
    fun selectAllBrokers(): List<StoredBrokerSettings>
}

@Database(entities = [(StoredBrokerSettings::class)], version = 1)
abstract class BrokersDb : RoomDatabase() {
    abstract fun brokersTable(): BrokersTable
}