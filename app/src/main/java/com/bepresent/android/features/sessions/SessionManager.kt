package com.bepresent.android.features.sessions

import android.content.Context
import com.bepresent.android.data.analytics.AnalyticsEvents
import com.bepresent.android.data.analytics.AnalyticsManager
import com.bepresent.android.data.convex.SyncManager
import com.bepresent.android.data.convex.SyncWorker
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.db.AppIntentionDao
import com.bepresent.android.data.db.PresentSession
import com.bepresent.android.data.db.PresentSessionAction
import com.bepresent.android.data.db.PresentSessionDao
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
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: PresentSessionDao,
    private val intentionDao: AppIntentionDao,
    private val preferencesManager: PreferencesManager,
    private val syncManager: SyncManager,
    private val sessionAlarmScheduler: SessionAlarmScheduler,
    private val analyticsManager: AnalyticsManager
) {
    fun observeActiveSession(): Flow<PresentSession?> = sessionDao.observeActiveSession()

    suspend fun getActiveSession(): PresentSession? = sessionDao.getActiveSession()

    suspend fun createAndStart(
        name: String,
        goalDurationMinutes: Int,
        blockedPackages: List<String>,
        beastMode: Boolean
    ): PresentSession {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val packagesJson = JSONArray(blockedPackages).toString()
        RuntimeLog.d(
            TAG,
            "createAndStart: name=$name duration=${goalDurationMinutes}m blocked=$blockedPackages json=$packagesJson"
        )

        val session = PresentSession(
            id = id,
            name = name,
            goalDurationMinutes = goalDurationMinutes,
            beastMode = beastMode,
            state = PresentSession.STATE_ACTIVE,
            blockedPackages = packagesJson,
            startedAt = now
        )

        sessionDao.upsert(session)
        sessionDao.insertAction(
            PresentSessionAction(
                id = UUID.randomUUID().toString(),
                sessionId = id,
                action = PresentSessionAction.ACTION_START
            )
        )
        preferencesManager.setActiveSessionId(id)
        sessionAlarmScheduler.scheduleGoalAlarm(id, now + (goalDurationMinutes * 60 * 1000L))
        RuntimeLog.d(TAG, "createAndStart: session saved, starting MonitoringService")
        MonitoringService.start(context)

        analyticsManager.track(
            AnalyticsEvents.STARTED_PRESENT_SESSION,
            mapOf(
                "session_id" to id,
                "goal_duration_minutes" to goalDurationMinutes,
                "beast_mode" to beastMode,
                "session_name" to name
            )
        )

        return session
    }

    suspend fun cancel(sessionId: String): Boolean =
        applyTransition(sessionId, SessionStateMachine::cancel)

    suspend fun giveUp(sessionId: String): Boolean =
        applyTransition(sessionId, SessionStateMachine::giveUp)

    suspend fun goalReached(sessionId: String): Boolean =
        applyTransition(sessionId, SessionStateMachine::goalReached)

    suspend fun complete(sessionId: String): Boolean =
        applyTransition(sessionId, SessionStateMachine::complete)

    fun getBlockedPackagesFromJson(json: String): Set<String> {
        return try {
            val array = JSONArray(json)
            val result = (0 until array.length()).map { array.getString(it) }.toSet()
            RuntimeLog.d(TAG, "parseBlockedJson: '$json' -> $result")
            result
        } catch (e: Exception) {
            RuntimeLog.e(TAG, "parseBlockedJson FAILED for: '$json'", e)
            emptySet()
        }
    }

    suspend fun getTotalBlockedTodayMs(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val now = System.currentTimeMillis()
        val sessions = sessionDao.getCompletedSessionsForDateRange(startOfDay, now)
        return sessions.sumOf { it.goalDurationMinutes.toLong() * 60_000L }
    }

    companion object {
        private const val TAG = "BP_Session"
    }

    private suspend fun applyTransition(
        sessionId: String,
        transitionFn: (PresentSession) -> SessionStateMachine.TransitionResult
    ): Boolean {
        val session = sessionDao.getById(sessionId) ?: return false
        RuntimeLog.d(TAG, "applyTransition: sessionId=$sessionId fromState=${session.state}")
        val result = transitionFn(session)
        if (result !is SessionStateMachine.TransitionResult.Success) {
            RuntimeLog.w(TAG, "applyTransition rejected: sessionId=$sessionId state=${session.state}")
            return false
        }

        val transition = result.transition
        val now = System.currentTimeMillis()
        val rewards = if (transition.rewardsEligible) {
            SessionStateMachine.calculateRewards(session.goalDurationMinutes)
        } else {
            null
        }

        val updated = session.copy(
            state = transition.newState,
            endedAt = if (transition.setEndedAt) now else session.endedAt,
            goalReachedAt = if (transition.setGoalReachedAt) now else session.goalReachedAt,
            earnedXp = rewards?.first ?: session.earnedXp,
            earnedCoins = rewards?.second ?: session.earnedCoins
        )

        sessionDao.upsert(updated)
        sessionDao.insertAction(
            PresentSessionAction(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                action = transition.action
            )
        )

        if (transition.setEndedAt) {
            val durationSeconds = ((updated.endedAt ?: now) - (session.startedAt ?: now)) / 1000L
            val outcome = when (updated.state) {
                PresentSession.STATE_COMPLETED -> "completed"
                PresentSession.STATE_GAVE_UP -> "gave_up"
                PresentSession.STATE_CANCELED -> "canceled"
                else -> updated.state
            }
            analyticsManager.track(
                AnalyticsEvents.ENDED_PRESENT_SESSION,
                mapOf(
                    "session_id" to sessionId,
                    "goal_duration_minutes" to session.goalDurationMinutes,
                    "beast_mode" to session.beastMode,
                    "session_name" to session.name,
                    "outcome" to outcome,
                    "earned_xp" to (updated.earnedXp),
                    "earned_coins" to (updated.earnedCoins),
                    "duration_seconds" to durationSeconds
                )
            )
        }

        if (transition.cancelAlarm) {
            sessionAlarmScheduler.cancelGoalAlarm(sessionId)
        }

        if (transition.rewardsEligible && rewards != null) {
            preferencesManager.addXpAndCoins(rewards.first, rewards.second)
        }

        if (transition.clearActiveSession) {
            preferencesManager.setActiveSessionId(null)
            MonitoringService.checkAndStop(context, sessionDao, intentionDao)
        }

        if (transition.syncAfter) {
            syncManager.enqueueSessionSync(updated)
            SyncWorker.triggerImmediateSync(context)
        }

        RuntimeLog.i(TAG, "applyTransition: sessionId=$sessionId toState=${updated.state}")
        return true
    }
}
