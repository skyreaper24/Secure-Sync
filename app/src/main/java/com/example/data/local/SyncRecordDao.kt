package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRecordDao {
    @Query("SELECT * FROM sync_records WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllRecords(): Flow<List<SyncRecord>>

    @Query("SELECT * FROM sync_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): SyncRecord?

    @Query("SELECT * FROM sync_records WHERE synced = 0")
    suspend fun getUnsyncedRecords(): List<SyncRecord>

    @Query("SELECT * FROM sync_records WHERE isDeleted = 0")
    suspend fun getAllRecordsList(): List<SyncRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SyncRecord)

    @Query("UPDATE sync_records SET isDeleted = 1, synced = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteRecord(id: String, timestamp: Long)

    @Query("DELETE FROM sync_records WHERE id = :id")
    suspend fun hardDeleteRecord(id: String)

    @Query("DELETE FROM sync_records")
    suspend fun clearAll()
}
