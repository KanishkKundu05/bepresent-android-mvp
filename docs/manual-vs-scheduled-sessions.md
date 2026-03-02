# Manual Sessions vs Scheduled Sessions

## Overview

BePresent supports two types of blocking sessions:

| Aspect | Manual Session | Scheduled Session |
|---|---|---|
| Trigger | User taps "Start Session" in app | Alarm fires at preset time |
| Timing | On-demand, any time | Recurring daily at fixed hours |
| Duration | User picks goal (e.g. 30 min) | Fixed window (e.g. 8–11 AM) |
| XP / Coins | Earned on goal completion | None (no gamification) |
| Beast Mode | Optional lock-in | Always blocks (no give-up) |
| State Machine | idle → active → goalReached → completed | enabled/disabled toggle only |
| Shield Type | `SHIELD_SESSION` / `SHIELD_GOAL_REACHED` | `SHIELD_SCHEDULE` |
| Stop Condition | User completes or gives up | Clock reaches end time |
| Persistence | Room `present_sessions` table | Room `scheduled_sessions` table |
| Boot Handling | Restart if state=active | Re-schedule alarms + check time window |

## Manual Session Flow

1. User opens app → picks apps to block + goal duration
2. `SessionManager.createAndStart()` saves to DB, starts `MonitoringService`
3. `SessionAlarmScheduler` sets exact alarm for goal time
4. During session: `MonitoringService` polls foreground app, shows `SHIELD_SESSION`
5. On goal alarm: state → `goalReached`, shield switches to `SHIELD_GOAL_REACHED`
6. User taps "Complete" → XP awarded, service stopped (if no other blockers)

## Scheduled Session Flow

1. User toggles "Morning Ninja" ON in Schedules tab
2. `ScheduledSessionManager.toggle()` saves enabled=true, schedules two alarms (start + stop)
3. At start time: `ScheduleAlarmReceiver` receives `ACTION_SCHEDULE_START`
   - Starts `MonitoringService`
   - Shows "Morning Ninja is active" notification
4. During window: `MonitoringService.getBlockedPackages()` includes schedule-blocked apps
   - Shield type: `SHIELD_SCHEDULE` (displays like session shield)
5. At stop time: `ScheduleAlarmReceiver` receives `ACTION_SCHEDULE_STOP`
   - Reschedules both alarms for next day
   - Stops service if no other blockers active
   - Shows "Morning Ninja ended" notification
6. On device reboot: `BootCompletedReceiver` reschedules all enabled session alarms

## Interaction Between Both

- `MonitoringService.getBlockedPackages()` merges packages from:
  1. Active manual session (`present_sessions`)
  2. App intentions (`app_intentions`)
  3. Active scheduled sessions (`scheduled_sessions` where current time is in window)
- Service stays running if ANY source has active blocks
- `checkAndStop()` checks all three sources before stopping
