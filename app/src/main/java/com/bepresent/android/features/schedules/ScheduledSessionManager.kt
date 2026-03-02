package com.bepresent.android.features.schedules

import android.content.Context
import com.bepresent.android.data.db.ScheduledSession
import com.bepresent.android.data.db.ScheduledSessionDao
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.service.MonitoringService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScheduledSessionDao,
    private val alarmScheduler: ScheduleAlarmScheduler
) {

    fun observeAll(): Flow<List<ScheduledSession>> = dao.getAll()

    suspend fun toggle(sessionId: String, enabled: Boolean) {
        val session = dao.getById(sessionId) ?: return
        RuntimeLog.i(TAG, "toggle: id=$sessionId enabled=$enabled")
        val updated = session.copy(enabled = enabled)
        dao.upsert(updated)
        if (enabled) {
            alarmScheduler.scheduleAlarms(updated)
            // If we're already inside the active window, start blocking immediately
            if (isCurrentlyActive(updated)) {
                RuntimeLog.i(TAG, "toggle: inside active window, starting MonitoringService now")
                MonitoringService.start(context)
            }
        } else {
            alarmScheduler.cancelAlarms(session)
        }
    }

    fun getBlockedPackagesFromJson(json: String): Set<String> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (e: Exception) {
            RuntimeLog.e(TAG, "parseBlockedJson FAILED for: '$json'", e)
            emptySet()
        }
    }

    fun isCurrentlyActive(session: ScheduledSession): Boolean {
        if (!session.enabled) return false
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = session.startHour * 60 + session.startMinute
        val endMinutes = session.endHour * 60 + session.endMinute
        return nowMinutes in startMinutes until endMinutes
    }

    suspend fun seedDefaults() {
        val existing = dao.getAllOnce()
        if (existing.isNotEmpty()) {
            RuntimeLog.d(TAG, "seedDefaults: ${existing.size} sessions already exist, skipping")
            return
        }

        RuntimeLog.i(TAG, "seedDefaults: inserting Morning Ninja")
        val blockedPackages = JSONArray(
            listOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",    // TikTok
                "com.twitter.android",          // X/Twitter
                "com.google.android.youtube",
                "com.reddit.frontpage",
                "com.snapchat.android",
                "com.facebook.katana"
            )
        ).toString()

        dao.upsert(
            ScheduledSession(
                id = UUID.randomUUID().toString(),
                name = "Morning Ninja",
                startHour = 8,
                startMinute = 0,
                endHour = 11,
                endMinute = 0,
                blockedPackages = blockedPackages,
                enabled = false
            )
        )
    }

    companion object {
        private const val TAG = "BP_SchedMgr"
    }
}
