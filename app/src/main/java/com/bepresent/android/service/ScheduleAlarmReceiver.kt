package com.bepresent.android.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bepresent.android.BePresentApp
import com.bepresent.android.data.db.BePresentDatabase
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.features.schedules.ScheduleAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
        RuntimeLog.i(TAG, "onReceive: action=${intent.action} scheduleId=$scheduleId")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    BePresentDatabase::class.java,
                    "bepresent.db"
                ).addMigrations(BePresentDatabase.MIGRATION_1_2, BePresentDatabase.MIGRATION_2_3)
                    .build()

                val dao = db.scheduledSessionDao()
                val session = dao.getById(scheduleId)

                when (intent.action) {
                    ACTION_SCHEDULE_START -> {
                        if (session != null && session.enabled) {
                            RuntimeLog.i(TAG, "ACTION_SCHEDULE_START: starting MonitoringService for ${session.name}")
                            MonitoringService.start(context)
                            showNotification(
                                context,
                                "${session.name} is active",
                                "Blocked apps are shielded until ${formatTime(session.endHour, session.endMinute)}",
                                NOTIFICATION_ID_SCHEDULE
                            )
                        } else {
                            RuntimeLog.w(TAG, "ACTION_SCHEDULE_START: session missing or disabled")
                        }
                    }
                    ACTION_SCHEDULE_STOP -> {
                        if (session != null) {
                            RuntimeLog.i(TAG, "ACTION_SCHEDULE_STOP: ${session.name} ended")

                            // Reschedule both alarms for next day
                            if (session.enabled) {
                                val scheduler = ScheduleAlarmScheduler(context)
                                scheduler.scheduleAlarms(session)
                            }

                            showNotification(
                                context,
                                "${session.name} ended",
                                "Blocked apps are now available",
                                NOTIFICATION_ID_SCHEDULE
                            )

                            // Check if service can stop
                            val hasActiveSession = db.presentSessionDao().getActiveSession() != null
                            val intentionCount = db.appIntentionDao().getCount()
                            val hasOtherActiveSchedules = dao.getEnabled().any { other ->
                                other.id != scheduleId && isCurrentlyActive(other)
                            }

                            RuntimeLog.i(TAG, "checkStop: session=$hasActiveSession intentions=$intentionCount otherSchedules=$hasOtherActiveSchedules")
                            if (!hasActiveSession && intentionCount == 0 && !hasOtherActiveSchedules) {
                                MonitoringService.stop(context)
                            }
                        } else {
                            RuntimeLog.w(TAG, "ACTION_SCHEDULE_STOP: session missing")
                        }
                    }
                }

                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isCurrentlyActive(session: com.bepresent.android.data.db.ScheduledSession): Boolean {
        val cal = java.util.Calendar.getInstance()
        val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val startMinutes = session.startHour * 60 + session.startMinute
        val endMinutes = session.endHour * 60 + session.endMinute
        return nowMinutes in startMinutes until endMinutes
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }

    private fun showNotification(context: Context, title: String, text: String, notifId: Int) {
        val notification = NotificationCompat.Builder(context, BePresentApp.CHANNEL_SESSION)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }

    companion object {
        private const val TAG = "BP_SchedReceiver"
        const val ACTION_SCHEDULE_START = "com.bepresent.android.SCHEDULE_START"
        const val ACTION_SCHEDULE_STOP = "com.bepresent.android.SCHEDULE_STOP"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        private const val NOTIFICATION_ID_SCHEDULE = 1003
    }
}
