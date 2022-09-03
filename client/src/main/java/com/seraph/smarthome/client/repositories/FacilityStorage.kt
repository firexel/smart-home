package com.seraph.smarthome.client.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
    fun getAll(): Flow<List<StoredFacility>>

    @Query("SELECT * FROM facility WHERE id = :id")
    suspend fun findFacility(id: String): StoredFacility?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: StoredFacility)
}

@Database(entities = [StoredFacility::class], version = 1)
abstract class FacilityDatabase : RoomDatabase() {
    abstract fun facilityDao(): FacilityDao
}

class FacilityStorage(
    private val context: Context,
    val database: FacilityDatabase
) {
    private val sharedPreferences: SharedPreferences
        get() = context.getSharedPreferences("facilities", Context.MODE_PRIVATE)

    var currentFacilityId: String?
        get() = sharedPreferences.getString("current", null)
        set(value) = sharedPreferences.edit().putString("current", value).apply()

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FacilityStorage? = null

        fun getInstance(context: Context): FacilityStorage {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context,
                    FacilityDatabase::class.java,
                    "facility_database"
                ).build()
                val instance = FacilityStorage(context.applicationContext, db)
                INSTANCE = instance
                instance
            }
        }
    }
}