package com.bepresent.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "session", "dailyStats", "intentions"
    val payload: String, // JSON serialized data
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    companion object {
        const val TYPE_SESSION = "session"
        const val TYPE_DAILY_STATS = "dailyStats"
        const val TYPE_INTENTIONS = "intentions"
        const val TYPE_APP_USAGE = "appUsage"
    }
}
