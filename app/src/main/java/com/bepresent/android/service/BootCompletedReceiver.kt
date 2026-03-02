package com.bepresent.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.data.db.BePresentDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        RuntimeLog.i(TAG, "onReceive: BOOT_COMPLETED")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    BePresentDatabase::class.java,
                    "bepresent.db"
                ).addMigrations(BePresentDatabase.MIGRATION_1_2, BePresentDatabase.MIGRATION_2_3)
                    .build()

                val hasIntentions = db.appIntentionDao().getCount() > 0
                val hasActiveSession = db.presentSessionDao().getActiveSession() != null

                // Check and reschedule enabled scheduled sessions
                val enabledSchedules = db.scheduledSessionDao().getEnabled()
                val hasEnabledSchedules = enabledSchedules.isNotEmpty()

                if (hasEnabledSchedules) {
                    RuntimeLog.i(TAG, "boot: rescheduling ${enabledSchedules.size} schedule alarms")
                    val scheduler = com.bepresent.android.features.schedules.ScheduleAlarmScheduler(context)
                    scheduler.rescheduleAll(enabledSchedules)
                }

                // Check if any schedule is currently in its active window
                val cal = java.util.Calendar.getInstance()
                val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                val hasActiveSchedule = enabledSchedules.any { session ->
                    val start = session.startHour * 60 + session.startMinute
                    val end = session.endHour * 60 + session.endMinute
                    nowMinutes in start until end
                }

                db.close()
                RuntimeLog.i(TAG, "boot check: intentions=$hasIntentions activeSession=$hasActiveSession schedules=$hasEnabledSchedules activeSchedule=$hasActiveSchedule")

                if (hasIntentions || hasActiveSession || hasActiveSchedule) {
                    RuntimeLog.i(TAG, "boot check: starting MonitoringService")
                    MonitoringService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BP_Boot"
    }
}
