package com.bepresent.android.data.convex

import kotlinx.serialization.Serializable

@Serializable
data class SessionSyncPayload(
    val localSessionId: String,
    val name: String,
    val goalDurationMinutes: Int,
    val state: String,
    val earnedXp: Int,
    val startedAt: Long,
    val endedAt: Long?
)

@Serializable
data class DailyStatsSyncPayload(
    val date: String,
    val totalXp: Int,
    val totalCoins: Int,
    val maxStreak: Int,
    val sessionsCompleted: Int,
    val totalFocusMinutes: Int
)

@Serializable
data class IntentionSnapshotPayload(
    val packageName: String,
    val appName: String,
    val streak: Int,
    val allowedOpensPerDay: Int,
    val totalOpensToday: Int
)

@Serializable
data class IntentionsSyncPayload(
    val intentions: List<IntentionSnapshotPayload>
)

@Serializable
data class AppUsageSyncEntry(
    val date: String,
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val openCount: Int
)

@Serializable
data class AppUsageSyncPayload(
    val entries: List<AppUsageSyncEntry>
)
