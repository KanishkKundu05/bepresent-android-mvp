package com.bepresent.android.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.bepresent.android.debug.RuntimeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class AppUsageInfo(
    val packageName: String,
    val totalTimeMs: Long
)

data class DailyAppUsage(
    val date: String,           // "2026-03-01"
    val packageName: String,
    val totalTimeMs: Long,
    val openCount: Int
)

data class MonthlyAppUsage(
    val yearMonth: String,      // "2026-03"
    val packageName: String,
    val totalTimeMs: Long
)

data class AppOpenEvent(
    val packageName: String,
    val timestamp: Long,
    val durationMs: Long?       // null if still in foreground
)

@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getTotalScreenTimeToday(): Long {
        val (startOfDay, now) = getTodayRange()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startOfDay, now
        )
        return stats.sumOf { it.totalTimeInForeground }
    }

    fun getPerAppScreenTime(): List<AppUsageInfo> {
        val (startOfDay, now) = getTodayRange()
        val aggregated = usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)
        return aggregated.values
            .filter { it.totalTimeInForeground > 0 }
            .map { AppUsageInfo(it.packageName, it.totalTimeInForeground) }
            .sortedByDescending { it.totalTimeMs }
    }

    fun getTopDistractingApp(): AppUsageInfo? {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
        val aggregated = usageStatsManager.queryAndAggregateUsageStats(weekAgo, now)
        return aggregated.values
            .filter { it.packageName in KNOWN_DISTRACTING_APPS && it.totalTimeInForeground > 0 }
            .maxByOrNull { it.totalTimeInForeground }
            ?.let { AppUsageInfo(it.packageName, it.totalTimeInForeground) }
    }

    fun detectForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000
        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var lastForegroundPackage: String? = null
        var eventCount = 0
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            eventCount++
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                lastForegroundPackage = event.packageName
            }
        }
        val result = lastForegroundPackage ?: getCurrentForegroundPackage()
        RuntimeLog.d(
            TAG,
            "detectForeground: events=$eventCount fromEvents=$lastForegroundPackage result=$result"
        )
        if (result == null) {
            RuntimeLog.w(TAG, "detectForeground: no foreground app detected in 10s window")
        }
        return result
    }

    /**
     * Per-app open counts + screen time for each of the last [days] days.
     * Uses queryEvents() to count ACTIVITY_RESUMED events and track foreground time.
     */
    fun getDailyAppUsage(days: Int = 7): List<DailyAppUsage> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
        val startMs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()

        // Track per-day, per-package: open count and foreground spans
        data class DayPackageKey(val date: String, val pkg: String)
        val openCounts = mutableMapOf<DayPackageKey, Int>()
        val foregroundTime = mutableMapOf<DayPackageKey, Long>()
        // Track when each package moved to foreground
        val foregroundStart = mutableMapOf<String, Long>()

        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val eventDate = java.time.Instant.ofEpochMilli(event.timeStamp)
                .atZone(zone).toLocalDate().format(dateFormatter)
            val key = DayPackageKey(eventDate, event.packageName)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    openCounts[key] = (openCounts[key] ?: 0) + 1
                    foregroundStart[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = foregroundStart.remove(event.packageName)
                    if (start != null) {
                        val duration = event.timeStamp - start
                        if (duration > 0) {
                            foregroundTime[key] = (foregroundTime[key] ?: 0L) + duration
                        }
                    }
                }
            }
        }

        // Close any still-in-foreground spans at current time
        for ((pkg, start) in foregroundStart) {
            val nowDate = today.format(dateFormatter)
            val key = DayPackageKey(nowDate, pkg)
            val duration = endMs - start
            if (duration > 0) {
                foregroundTime[key] = (foregroundTime[key] ?: 0L) + duration
            }
        }

        val allKeys = openCounts.keys + foregroundTime.keys
        return allKeys.distinct().map { key ->
            DailyAppUsage(
                date = key.date,
                packageName = key.pkg,
                totalTimeMs = foregroundTime[key] ?: 0L,
                openCount = openCounts[key] ?: 0
            )
        }.sortedWith(compareBy({ it.date }, { -it.totalTimeMs }))
    }

    /**
     * Per-app monthly totals for the last [months] months.
     * Uses INTERVAL_MONTHLY for efficient aggregation.
     */
    fun getMonthlyAppUsage(months: Int = 6): List<MonthlyAppUsage> {
        val zone = ZoneId.systemDefault()
        val startMonth = YearMonth.now().minusMonths(months.toLong() - 1)
        val startMs = startMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_MONTHLY, startMs, endMs
        )

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { stat ->
                val yearMonth = java.time.Instant.ofEpochMilli(stat.firstTimeStamp)
                    .atZone(zone).toLocalDate().let { YearMonth.from(it) }
                MonthlyAppUsage(
                    yearMonth = yearMonth.toString(), // "2026-03"
                    packageName = stat.packageName,
                    totalTimeMs = stat.totalTimeInForeground
                )
            }
            .sortedWith(compareBy({ it.yearMonth }, { -it.totalTimeMs }))
    }

    /**
     * Exact app-open events with timestamps for the last [days] days.
     * Pairs ACTIVITY_RESUMED with ACTIVITY_PAUSED to compute duration.
     */
    fun getAppOpenEvents(days: Int = 3): List<AppOpenEvent> {
        val zone = ZoneId.systemDefault()
        val startMs = LocalDate.now().minusDays(days.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()

        // Track foreground start per package
        val foregroundStart = mutableMapOf<String, Long>()
        val result = mutableListOf<AppOpenEvent>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundStart[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = foregroundStart.remove(event.packageName)
                    if (start != null) {
                        result.add(
                            AppOpenEvent(
                                packageName = event.packageName,
                                timestamp = start,
                                durationMs = event.timeStamp - start
                            )
                        )
                    }
                }
            }
        }

        // Add still-in-foreground apps with null duration
        for ((pkg, start) in foregroundStart) {
            result.add(AppOpenEvent(packageName = pkg, timestamp = start, durationMs = null))
        }

        return result.sortedByDescending { it.timestamp }
    }

    companion object {
        private const val TAG = "BP_Usage"

        val KNOWN_DISTRACTING_APPS = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",    // TikTok
            "com.twitter.android",          // X/Twitter
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.facebook.katana"
        )
    }

    private fun getCurrentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - 10_000, now
        )
        return stats
            .filter { it.lastTimeUsed > now - 10_000 }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis to System.currentTimeMillis()
    }
}
