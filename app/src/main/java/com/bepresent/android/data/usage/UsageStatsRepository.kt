package com.bepresent.android.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.bepresent.android.debug.RuntimeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class AppUsageInfo(
    val packageName: String,
    val totalTimeMs: Long
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

    companion object {
        private const val TAG = "BP_Usage"
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
