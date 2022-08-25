package com.seraph.smarthome.client.repositories

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Suppress("ArrayInDataClass")
@Entity(tableName = "facility")
data class StoredFacility(
    @PrimaryKey val id: String,
    @NonNull @ColumnInfo(name = "name") val name: String,
    @NonNull @ColumnInfo(name = "cover") val cover: String,
    @NonNull @ColumnInfo(name = "host") val brokerHost: String,
    @NonNull @ColumnInfo(name = "port") val brokerPort: Int,
    @Nullable @ColumnInfo(name = "login") val brokerLogin: String?,
    @Nullable @ColumnInfo(name = "password") val brokerPassword: String?
)

@Dao
interface FacilityDao {
    @Query("SELECT * FROM facility")
    suspend fun getAll(): Flow<List<StoredFacility>>

    @Query("SELECT * FROM facility WHERE id = :id")
    suspend fun findFacility(id: String): StoredFacility?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: StoredFacility)
}

abstract class FacilityDatabase : RoomDatabase() {
    abstract fun facilityDao(): FacilityDao

    companion object {
        @Volatile
        private var INSTANCE: FacilityDatabase? = null

        fun getDatabase(context: Context): FacilityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    FacilityDatabase::class.java,
                    "facility_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}