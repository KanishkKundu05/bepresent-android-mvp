package com.bepresent.android.features.intentions

import android.content.Context
import com.bepresent.android.data.analytics.AnalyticsEvents
import com.bepresent.android.data.analytics.AnalyticsManager
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.data.db.AppIntentionDao
import com.bepresent.android.data.db.PresentSessionDao
import com.bepresent.android.service.MonitoringService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentionDao: AppIntentionDao,
    private val sessionDao: PresentSessionDao,
    private val intentionAlarmScheduler: IntentionAlarmScheduler,
    private val analyticsManager: AnalyticsManager
) {
    fun observeAll(): Flow<List<AppIntention>> = intentionDao.getAll()

    suspend fun getAll(): List<AppIntention> = intentionDao.getAllOnce()

    suspend fun getByPackage(packageName: String): AppIntention? =
        intentionDao.getByPackage(packageName)

    suspend fun getById(id: String): AppIntention? = intentionDao.getById(id)

    suspend fun create(
        packageName: String,
        appName: String,
        allowedOpensPerDay: Int,
        timePerOpenMinutes: Int
    ): AppIntention {
        RuntimeLog.i(
            TAG,
            "create: package=$packageName app=$appName allowed=$allowedOpensPerDay window=${timePerOpenMinutes}m"
        )
        val intention = AppIntention(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = appName,
            allowedOpensPerDay = allowedOpensPerDay,
            timePerOpenMinutes = timePerOpenMinutes
        )
        intentionDao.upsert(intention)
        ensureMonitoringServiceRunning()
        analyticsManager.track(
            AnalyticsEvents.SET_APP_INTENTION,
            mapOf(
                "package_name" to packageName,
                "app_name" to appName,
                "opens" to allowedOpensPerDay,
                "min_per_open" to timePerOpenMinutes,
                "from" to "home"
            )
        )
        return intention
    }

    suspend fun update(intention: AppIntention) {
        val old = intentionDao.getById(intention.id)
        RuntimeLog.i(TAG, "update: id=${intention.id} package=${intention.packageName}")
        intentionDao.upsert(intention)
        if (old != null) {
            analyticsManager.track(
                AnalyticsEvents.MODIFIED_APP_INTENTION,
                mapOf(
                    "package_name" to intention.packageName,
                    "app_name" to intention.appName,
                    "old_opens" to old.allowedOpensPerDay,
                    "new_opens" to intention.allowedOpensPerDay,
                    "old_min_per_open" to old.timePerOpenMinutes,
                    "new_min_per_open" to intention.timePerOpenMinutes
                )
            )
        }
    }

    suspend fun delete(intention: AppIntention) {
        RuntimeLog.i(TAG, "delete: id=${intention.id} package=${intention.packageName}")
        // Cancel any active alarm for this intention
        intentionAlarmScheduler.cancelReblock(intention.id)
        intentionDao.delete(intention)
        analyticsManager.track(
            AnalyticsEvents.REMOVED_APP_INTENTION,
            mapOf(
                "package_name" to intention.packageName,
                "app_name" to intention.appName,
                "opens" to intention.allowedOpensPerDay,
                "min_per_open" to intention.timePerOpenMinutes
            )
        )

        // Stop service if no more intentions and no active session
        MonitoringService.checkAndStop(context, sessionDao, intentionDao)
    }

    suspend fun openApp(intentionId: String) {
        val intention = intentionDao.getById(intentionId) ?: run {
            RuntimeLog.w(TAG, "openApp: intention not found id=$intentionId")
            return
        }
        RuntimeLog.i(
            TAG,
            "openApp: id=$intentionId opens=${intention.totalOpensToday}/${intention.allowedOpensPerDay} window=${intention.timePerOpenMinutes}m"
        )
        analyticsManager.track(AnalyticsEvents.HAS_APP_INTENTION)
        intentionDao.incrementOpens(intentionId)
        intentionDao.setOpenState(intentionId, true, System.currentTimeMillis())
        intentionAlarmScheduler.scheduleReblock(intentionId, intention.timePerOpenMinutes)
    }

    suspend fun reblockApp(intentionId: String) {
        RuntimeLog.i(TAG, "reblockApp: id=$intentionId")
        intentionDao.setOpenState(intentionId, false, null)
    }

    suspend fun getBlockedPackages(): Set<String> {
        return intentionDao.getBlockedIntentions()
            .map { it.packageName }
            .toSet()
    }

    private fun ensureMonitoringServiceRunning() {
        RuntimeLog.d(TAG, "ensureMonitoringServiceRunning")
        MonitoringService.start(context)
    }

    companion object {
        private const val TAG = "BP_Intention"
    }
}
