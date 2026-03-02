package com.bepresent.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_sessions")
data class ScheduledSession(
    @PrimaryKey val id: String,
    val name: String,
    val startHour: Int,      // 0-23
    val startMinute: Int,    // 0-59
    val endHour: Int,
    val endMinute: Int,
    val blockedPackages: String,  // JSON array
    val enabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
