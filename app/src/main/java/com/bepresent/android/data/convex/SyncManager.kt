package com.bepresent.android.data.convex

import android.content.Context
import android.util.Log
import com.bepresent.android.data.db.AppIntentionDao
import com.bepresent.android.data.db.PresentSession
import com.bepresent.android.data.db.PresentSessionDao
import com.bepresent.android.data.db.SyncQueueDao
import com.bepresent.android.data.db.SyncQueueItem
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.usage.UsageStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncQueueDao: SyncQueueDao,
    private val sessionDao: PresentSessionDao,
    private val intentionDao: AppIntentionDao,
    private val preferencesManager: PreferencesManager,
    private val convexManager: ConvexManager,
    private val usageStatsRepository: UsageStatsRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enqueueSessionSync(session: PresentSession) {
        val payload = SessionSyncPayload(
            localSessionId = session.id,
            name = session.name,
            goalDurationMinutes = session.goalDurationMinutes,
            state = session.state,
            earnedXp = session.earnedXp,
            startedAt = session.startedAt ?: System.currentTimeMillis(),
            endedAt = session.endedAt
        )
        syncQueueDao.insert(
            SyncQueueItem(
                type = SyncQueueItem.TYPE_SESSION,
                payload = json.encodeToString(payload)
            )
        )
    }

    suspend fun enqueueDailyStatsSync(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = sessionDao.getCompletedSessionsForDate(dayStart, dayEnd)
        val totalXp = preferencesManager.totalXp.first()
        val totalCoins = preferencesManager.totalCoins.first()
        val intentions = intentionDao.getAllOnce()
        val maxStreak = intentions.maxOfOrNull { it.streak } ?: 0
        val completedSessions = sessions.count { it.state == PresentSession.STATE_COMPLETED }
        val totalFocusMinutes = sessions
            .filter { it.state == PresentSession.STATE_COMPLETED }
            .sumOf { it.goalDurationMinutes }

        val payload = DailyStatsSyncPayload(
            date = date.toString(),
            totalXp = totalXp,
            totalCoins = totalCoins,
            maxStreak = maxStreak,
            sessionsCompleted = completedSessions,
            totalFocusMinutes = totalFocusMinutes
        )
        syncQueueDao.insert(
            SyncQueueItem(
                type = SyncQueueItem.TYPE_DAILY_STATS,
                payload = json.encodeToString(payload)
            )
        )
    }

    suspend fun enqueueAppUsageSync() {
        val dailyUsage = usageStatsRepository.getDailyAppUsage(7)
        val pm = context.packageManager
        val entries = dailyUsage.map { usage ->
            val appName = try {
                val ai = pm.getApplicationInfo(usage.packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                usage.packageName
            }
            AppUsageSyncEntry(
                date = usage.date,
                packageName = usage.packageName,
                appName = appName,
                totalTimeMs = usage.totalTimeMs,
                openCount = usage.openCount
            )
        }
        val payload = AppUsageSyncPayload(entries = entries)
        syncQueueDao.insert(
            SyncQueueItem(
                type = SyncQueueItem.TYPE_APP_USAGE,
                payload = json.encodeToString(payload)
            )
        )
    }

    suspend fun enqueueIntentionsSync() {
        val intentions = intentionDao.getAllOnce()
        val snapshots = intentions.map { intention ->
            IntentionSnapshotPayload(
                packageName = intention.packageName,
                appName = intention.appName,
                streak = intention.streak,
                allowedOpensPerDay = intention.allowedOpensPerDay,
                totalOpensToday = intention.totalOpensToday
            )
        }
        val payload = IntentionsSyncPayload(intentions = snapshots)
        syncQueueDao.insert(
            SyncQueueItem(
                type = SyncQueueItem.TYPE_INTENTIONS,
                payload = json.encodeToString(payload)
            )
        )
    }

    suspend fun processQueue() {
        if (!convexManager.isAuthenticated) return

        // Clean up items that have failed too many times
        syncQueueDao.deleteFailedItems()

        val items = syncQueueDao.getAll()
        val client = convexManager.client ?: return

        for (item in items) {
            try {
                when (item.type) {
                    SyncQueueItem.TYPE_SESSION -> {
                        val payload = json.decodeFromString<SessionSyncPayload>(item.payload)
                        client.mutation<Unit>(
                            "stats:syncSession",
                            args = mapOf(
                                "localSessionId" to payload.localSessionId,
                                "name" to payload.name,
                                "goalDurationMinutes" to payload.goalDurationMinutes,
                                "state" to payload.state,
                                "earnedXp" to payload.earnedXp,
                                "startedAt" to payload.startedAt,
                                "endedAt" to payload.endedAt
                            )
                        )
                        syncQueueDao.delete(item)
                    }

                    SyncQueueItem.TYPE_DAILY_STATS -> {
                        val payload = json.decodeFromString<DailyStatsSyncPayload>(item.payload)
                        client.mutation<Unit>(
                            "stats:syncDailyStats",
                            args = mapOf(
                                "date" to payload.date,
                                "totalXp" to payload.totalXp,
                                "totalCoins" to payload.totalCoins,
                                "maxStreak" to payload.maxStreak,
                                "sessionsCompleted" to payload.sessionsCompleted,
                                "totalFocusMinutes" to payload.totalFocusMinutes
                            )
                        )
                        syncQueueDao.delete(item)
                    }

                    SyncQueueItem.TYPE_INTENTIONS -> {
                        val payload = json.decodeFromString<IntentionsSyncPayload>(item.payload)
                        val intentionMaps = payload.intentions.map { i ->
                            mapOf(
                                "packageName" to i.packageName,
                                "appName" to i.appName,
                                "streak" to i.streak,
                                "allowedOpensPerDay" to i.allowedOpensPerDay,
                                "totalOpensToday" to i.totalOpensToday
                            )
                        }
                        client.mutation<Unit>(
                            "stats:syncIntentions",
                            args = mapOf("intentions" to intentionMaps)
                        )
                        syncQueueDao.delete(item)
                    }

                    SyncQueueItem.TYPE_APP_USAGE -> {
                        val payload = json.decodeFromString<AppUsageSyncPayload>(item.payload)
                        val entryMaps = payload.entries.map { e ->
                            mapOf(
                                "date" to e.date,
                                "packageName" to e.packageName,
                                "appName" to e.appName,
                                "totalTimeMs" to e.totalTimeMs,
                                "openCount" to e.openCount
                            )
                        }
                        client.mutation<Unit>(
                            "stats:syncAppUsage",
                            args = mapOf("entries" to entryMaps)
                        )
                        syncQueueDao.delete(item)
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Failed to sync item ${item.id}: ${e.message}")
                syncQueueDao.incrementRetry(item.id)
            }
        }
    }
}
