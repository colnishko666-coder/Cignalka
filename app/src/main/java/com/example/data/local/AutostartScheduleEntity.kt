package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autostart_schedules")
data class AutostartScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val warmupMinutes: Int,
    val daysMask: Int, // Bitmask Mon=1..Sun=64; 0 means once
    val isEnabled: Boolean = true,
    val lastTriggeredDayKey: String = ""
)
