# BePresent Android — Permissions Reference

| Permission | Manifest Name | When Requested | What It Enables |
|---|---|---|---|
| Usage Stats | `PACKAGE_USAGE_STATS` | Onboarding (system settings) | Detect which app is in the foreground for blocking |
| Query All Packages | `QUERY_ALL_PACKAGES` | Install-time (automatic) | Enumerate installed apps for the app picker |
| Post Notifications | `POST_NOTIFICATIONS` | Onboarding (runtime, Android 13+) | Session, intention, and schedule notifications |
| Foreground Service | `FOREGROUND_SERVICE` | Install-time (automatic) | Run MonitoringService as a persistent foreground service |
| Foreground Service (Special Use) | `FOREGROUND_SERVICE_SPECIAL_USE` | Install-time (automatic) | Declare "digital wellbeing" as the special-use FGS subtype |
| Ignore Battery Optimizations | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Onboarding (system dialog) | Prevent Doze from killing the monitoring service |
| Boot Completed | `RECEIVE_BOOT_COMPLETED` | Install-time (automatic) | Re-schedule alarms and restart service after device reboot |
| Internet | `INTERNET` | Install-time (automatic) | Cloud sync (Convex) and Auth0 login |
| Read Contacts | `READ_CONTACTS` | Runtime (when adding partner) | Contact picker for accountability partners |
| Exact Alarm | `USE_EXACT_ALARM` | Install-time (Android 12+) | Fire alarms at exact times for session goals, intention re-blocks, and scheduled session start/stop |
| Wake Lock | `WAKE_LOCK` | Install-time (automatic) | Keep CPU awake during alarm handling |
| System Alert Window (Overlay) | `SYSTEM_ALERT_WINDOW` | Onboarding (system settings) | Launch BlockedAppActivity shield from background service |

## Notes

- All permissions are requested during onboarding. No additional permissions are needed for scheduled sessions.
- `PACKAGE_USAGE_STATS` and `SYSTEM_ALERT_WINDOW` require navigating to system settings screens.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` shows a system dialog.
- `POST_NOTIFICATIONS` is a runtime permission on Android 13+ only.
