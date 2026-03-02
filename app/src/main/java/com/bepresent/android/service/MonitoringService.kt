package com.bepresent.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bepresent.android.BePresentApp
import com.bepresent.android.MainActivity
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.data.db.AppIntentionDao
import com.bepresent.android.data.db.PresentSession
import com.bepresent.android.data.db.PresentSessionDao
import com.bepresent.android.data.db.ScheduledSessionDao
import com.bepresent.android.data.usage.UsageStatsRepository
import com.bepresent.android.features.blocking.BlockedAppActivity
import com.bepresent.android.features.schedules.ScheduledSessionManager
import com.bepresent.android.features.sessions.SessionManager
import com.bepresent.android.permissions.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var usageStatsRepository: UsageStatsRepository
    @Inject lateinit var intentionDao: AppIntentionDao
    @Inject lateinit var sessionDao: PresentSessionDao
    @Inject lateinit var scheduledSessionDao: ScheduledSessionDao
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var scheduledSessionManager: ScheduledSessionManager
    @Inject lateinit var permissionManager: PermissionManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0
    private var lastKnownForegroundPackage: String? = null
    private var lastDebugNotificationTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val permissions = permissionManager.checkAll()
        RuntimeLog.i(
            TAG,
            "onStartCommand: usage=${permissions.usageStats} overlay=${permissions.overlay} " +
                "a11y=${permissions.accessibility} notif=${permissions.notifications} " +
                "battery=${permissions.batteryOptimization}"
        )
        startForeground(NOTIFICATION_ID, createMonitoringNotification())
        startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) {
            RuntimeLog.d(TAG, "startPolling: job already active, skipping")
            return
        }
        RuntimeLog.d(TAG, "startPolling: launching polling coroutine")
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val detected = usageStatsRepository.detectForegroundApp()
                    val foregroundPackage = if (detected != null) {
                        lastKnownForegroundPackage = detected
                        detected
                    } else {
                        lastKnownForegroundPackage
                    }
                    RuntimeLog.d(TAG, "poll: detected=$detected foreground=$foregroundPackage")
                    if (foregroundPackage != null && foregroundPackage != packageName) {
                        val blockedPackages = getBlockedPackages()
                        RuntimeLog.d(TAG, "poll: blockedPackages=$blockedPackages")
                        if (foregroundPackage in blockedPackages) {
                            val now = System.currentTimeMillis()
                            if (foregroundPackage != lastBlockedPackage || now - lastBlockedTime > 2000) {
                                lastBlockedPackage = foregroundPackage
                                lastBlockedTime = now
                                val shieldType = determineShieldType(foregroundPackage)
                                RuntimeLog.w(TAG, "BLOCKING $foregroundPackage shield=$shieldType")
                                launchBlockedActivity(foregroundPackage, shieldType)
                            } else {
                                RuntimeLog.d(TAG, "poll: debounced block for $foregroundPackage")
                            }
                        }
                    }
                } catch (e: Exception) {
                    RuntimeLog.e(TAG, "Polling error", e)
                }
                delay(1000)
            }
        }
    }

    private suspend fun getBlockedPackages(): Set<String> {
        val activeSession = sessionDao.getActiveSession()
        val sessionBlocked = activeSession?.let { session ->
            RuntimeLog.d(
                TAG,
                "getBlocked: session=${session.id} state=${session.state} json=${session.blockedPackages}"
            )
            sessionManager.getBlockedPackagesFromJson(session.blockedPackages)
        } ?: emptySet()

        val intentionBlocked = intentionDao.getBlockedIntentions()
            .map { it.packageName }
            .toSet()

        val scheduleBlocked = getScheduleBlockedPackages()

        val combined = sessionBlocked + intentionBlocked + scheduleBlocked
        if (combined.isNotEmpty()) {
            RuntimeLog.d(TAG, "getBlocked: session=$sessionBlocked intention=$intentionBlocked schedule=$scheduleBlocked")
        }
        return combined
    }

    private suspend fun getScheduleBlockedPackages(): Set<String> {
        val enabledSessions = scheduledSessionDao.getEnabled()
        val blocked = mutableSetOf<String>()
        for (session in enabledSessions) {
            if (scheduledSessionManager.isCurrentlyActive(session)) {
                blocked.addAll(scheduledSessionManager.getBlockedPackagesFromJson(session.blockedPackages))
            }
        }
        return blocked
    }

    private suspend fun determineShieldType(packageName: String): String {
        // Session takes priority over schedule, which takes priority over intention
        val activeSession = sessionDao.getActiveSession()
        if (activeSession != null) {
            val sessionPackages = sessionManager.getBlockedPackagesFromJson(activeSession.blockedPackages)
            if (packageName in sessionPackages) {
                return if (activeSession.state == PresentSession.STATE_GOAL_REACHED) {
                    BlockedAppActivity.SHIELD_GOAL_REACHED
                } else {
                    BlockedAppActivity.SHIELD_SESSION
                }
            }
        }

        // Check scheduled sessions
        val scheduleBlocked = getScheduleBlockedPackages()
        if (packageName in scheduleBlocked) {
            return BlockedAppActivity.SHIELD_SCHEDULE
        }

        return BlockedAppActivity.SHIELD_INTENTION
    }

    private fun launchBlockedActivity(packageName: String, shieldType: String) {
        val launchRequestedAt = System.currentTimeMillis()
        RuntimeLog.w(
            TAG,
            "launchBlockedActivity: pkg=$packageName shield=$shieldType overlay=${android.provider.Settings.canDrawOverlays(this)}"
        )
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            putExtra(BlockedAppActivity.EXTRA_SHIELD_TYPE, shieldType)
        }
        try {
            startActivity(intent)
            RuntimeLog.i(TAG, "launchBlockedActivity: startActivity requested")
            serviceScope.launch {
                delay(1200)
                if (BlockedAppActivity.lastResumeAtMs < launchRequestedAt) {
                    RuntimeLog.w(
                        TAG,
                        "Shield did not resume after launch request. Android may have blocked background activity start."
                    )
                    showShieldDebugNotification(packageName, shieldType)
                }
            }
        } catch (t: Throwable) {
            RuntimeLog.e(TAG, "launchBlockedActivity failed", t)
            showShieldDebugNotification(packageName, shieldType)
        }
    }

    private fun showShieldDebugNotification(packageName: String, shieldType: String) {
        val now = System.currentTimeMillis()
        if (now - lastDebugNotificationTime < 5000) return
        lastDebugNotificationTime = now

        val openShieldIntent = Intent(this, BlockedAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            putExtra(BlockedAppActivity.EXTRA_SHIELD_TYPE, shieldType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (packageName + shieldType).hashCode(),
            openShieldIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, BePresentApp.CHANNEL_MONITORING)
            .setContentTitle("Shield launch needs user tap")
            .setContentText("Tap to open shield for ${packageName.substringAfterLast('.')}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(DEBUG_NOTIFICATION_ID, notification)
    }

    private fun createMonitoringNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BePresentApp.CHANNEL_MONITORING)
            .setContentTitle("BePresent is active")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateNotificationForSession(session: PresentSession) {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (session.state == PresentSession.STATE_GOAL_REACHED) {
            val (xp, _) = com.bepresent.android.features.sessions.SessionStateMachine.calculateRewards(session.goalDurationMinutes)
            NotificationCompat.Builder(this, BePresentApp.CHANNEL_SESSION)
                .setContentTitle("Goal Reached!")
                .setContentText("+$xp XP — tap to complete")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        } else {
            val elapsed = System.currentTimeMillis() - (session.startedAt ?: System.currentTimeMillis())
            val remaining = (session.goalDurationMinutes * 60 * 1000L) - elapsed
            val remainingMinutes = (remaining / 60000).coerceAtLeast(0)

            NotificationCompat.Builder(this, BePresentApp.CHANNEL_SESSION)
                .setContentTitle(session.name)
                .setContentText("${remainingMinutes}m remaining")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + remaining)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "BP_Monitor"
        const val NOTIFICATION_ID = 1001
        private const val DEBUG_NOTIFICATION_ID = 1002

        fun start(context: Context) {
            RuntimeLog.i(TAG, "start() requested")
            val intent = Intent(context, MonitoringService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            RuntimeLog.i(TAG, "stop() requested")
            context.stopService(Intent(context, MonitoringService::class.java))
        }

        fun checkAndStop(
            context: Context,
            sessionDao: PresentSessionDao,
            intentionDao: AppIntentionDao,
            scheduledSessionDao: ScheduledSessionDao? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val hasActiveSession = sessionDao.getActiveSession() != null
                val intentionCount = intentionDao.getCount()
                val hasActiveSchedule = scheduledSessionDao?.let { dao ->
                    val cal = java.util.Calendar.getInstance()
                    val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                    dao.getEnabled().any { session ->
                        val start = session.startHour * 60 + session.startMinute
                        val end = session.endHour * 60 + session.endMinute
                        nowMinutes in start until end
                    }
                } ?: false
                RuntimeLog.i(
                    TAG,
                    "checkAndStop: hasActiveSession=$hasActiveSession intentionCount=$intentionCount hasActiveSchedule=$hasActiveSchedule"
                )
                if (!hasActiveSession && intentionCount == 0 && !hasActiveSchedule) {
                    stop(context)
                }
            }
        }
    }
}
