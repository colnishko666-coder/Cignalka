package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs ORDER BY timestampMillis DESC LIMIT 60")
    fun observeRecentLogs(): Flow<List<EventLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: EventLogEntity)

    @Query("DELETE FROM event_logs")
    suspend fun clearAll()
}
