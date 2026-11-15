package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutostartScheduleDao {
    @Query("SELECT * FROM autostart_schedules ORDER BY hour ASC, minute ASC")
    fun observeSchedules(): Flow<List<AutostartScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: AutostartScheduleEntity)

    @Update
    suspend fun update(schedule: AutostartScheduleEntity)

    @Query("UPDATE autostart_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE autostart_schedules SET lastTriggeredDayKey = :dayKey WHERE id = :id")
    suspend fun markTriggered(id: Long, dayKey: String)

    @Query("DELETE FROM autostart_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
