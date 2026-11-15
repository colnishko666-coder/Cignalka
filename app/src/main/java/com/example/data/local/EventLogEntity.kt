package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val detail: String,
    val transport: String, // "BLE" or "Симуляция" or "Расписание" or "Датчик"
    val isAlert: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)
