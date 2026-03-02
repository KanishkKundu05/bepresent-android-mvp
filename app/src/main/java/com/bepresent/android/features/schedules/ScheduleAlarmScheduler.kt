package com.bepresent.android.features.schedules

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bepresent.android.data.db.ScheduledSession
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.service.ScheduleAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleAlarms(session: ScheduledSession) {
        RuntimeLog.i(TAG, "scheduleAlarms: id=${session.id} ${session.startHour}:${session.startMinute}-${session.endHour}:${session.endMinute}")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // START alarm
        val startTime = nextTriggerTime(session.startHour, session.startMinute)
        val startIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_SCHEDULE_START
            putExtra(ScheduleAlarmReceiver.EXTRA_SCHEDULE_ID, session.id)
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            session.id.hashCode(),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(startTime, null), startPending)
        RuntimeLog.i(TAG, "scheduleAlarms: START alarm at $startTime")

        // STOP alarm
        val stopTime = nextTriggerTime(session.endHour, session.endMinute)
        val stopIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_SCHEDULE_STOP
            putExtra(ScheduleAlarmReceiver.EXTRA_SCHEDULE_ID, session.id)
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            session.id.hashCode() + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(stopTime, null), stopPending)
        RuntimeLog.i(TAG, "scheduleAlarms: STOP alarm at $stopTime")
    }

    fun cancelAlarms(session: ScheduledSession) {
        RuntimeLog.i(TAG, "cancelAlarms: id=${session.id}")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(context, ScheduleAlarmReceiver::class.java)
        val startPending = PendingIntent.getBroadcast(
            context,
            session.id.hashCode(),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(startPending)

        val stopIntent = Intent(context, ScheduleAlarmReceiver::class.java)
        val stopPending = PendingIntent.getBroadcast(
            context,
            session.id.hashCode() + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(stopPending)
    }

    fun rescheduleAll(sessions: List<ScheduledSession>) {
        RuntimeLog.i(TAG, "rescheduleAll: ${sessions.size} enabled sessions")
        sessions.forEach { scheduleAlarms(it) }
    }

    companion object {
        private const val TAG = "BP_SchedAlarm"

        /**
         * Returns epoch ms for the next occurrence of [hour]:[minute].
         * If that time has already passed today, returns tomorrow's occurrence.
         */
        fun nextTriggerTime(hour: Int, minute: Int): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }
    }
}
