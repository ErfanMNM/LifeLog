package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val triggerTime: Long, // Millis timestamp
    val isCompleted: Boolean = false,
    val enableSound: Boolean = true,
    val enableVibration: Boolean = true,
    val createdTimestamp: Long = System.currentTimeMillis()
)
