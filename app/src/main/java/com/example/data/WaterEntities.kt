package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterEntry)

    @Query("SELECT * FROM water_entries WHERE timestampMillis >= :startOfDay ORDER BY timestampMillis DESC")
    fun getTodayEntries(startOfDay: Long): Flow<List<WaterEntry>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE timestampMillis >= :startOfDay")
    fun getTodayTotal(startOfDay: Long): Flow<Int>

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM water_entries WHERE timestampMillis >= :startOfDay")
    suspend fun clearToday(startOfDay: Long)
}
