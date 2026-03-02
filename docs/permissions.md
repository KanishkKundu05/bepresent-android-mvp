# BePresent Android — Permissions Reference

## How Blocking Works

BePresent does **not** need per-app permissions to block apps. The mechanism is:

1. `MonitoringService` polls `UsageStatsManager` every 1 second to detect the foreground app
2. If the foreground app is in the blocked set, `BlockedAppActivity` is launched on top as a shield overlay
3. The user must dismiss the shield (which sends them home) — they cannot return to the blocked app

This requires three core permissions: **Usage Stats** (detect foreground app), **Overlay** (launch shield from background), and **Accessibility Service** (reliable foreground detection). No device admin, MDM, or root access is needed.

## Permission Table

| Permission | Manifest Name | Grant Type | When Requested | What It Enables |
|---|---|---|---|---|
| Usage Stats | `PACKAGE_USAGE_STATS` | System settings | Onboarding | Detect which app is in the foreground via `UsageStatsManager` |
| Overlay | `SYSTEM_ALERT_WINDOW` | System settings | Onboarding | Launch `BlockedAppActivity` shield from `MonitoringService` (background) |
| Accessibility Service | `BIND_ACCESSIBILITY_SERVICE` | System settings | Onboarding | `AccessibilityMonitorService` for reliable foreground app detection |
| Post Notifications | `POST_NOTIFICATIONS` | Runtime (Android 13+) | Onboarding | Session, intention, and schedule notifications |
| Battery Optimization Exemption | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | System dialog | Onboarding | Prevent Doze from killing `MonitoringService` |
| Foreground Service | `FOREGROUND_SERVICE` | Install-time | Automatic | Run `MonitoringService` as a persistent foreground service |
| Foreground Service (Special Use) | `FOREGROUND_SERVICE_SPECIAL_USE` | Install-time | Automatic | Declare "digital wellbeing" as the special-use FGS subtype |
| Boot Completed | `RECEIVE_BOOT_COMPLETED` | Install-time | Automatic | Reschedule alarms and restart service after device reboot |
| Exact Alarm | `USE_EXACT_ALARM` | Install-time (Android 12+) | Automatic | Fire alarms at exact times for session goals, intention re-blocks, and scheduled session start/stop |
| Wake Lock | `WAKE_LOCK` | Install-time | Automatic | Keep CPU awake during alarm handling in broadcast receivers |
| Query All Packages | `QUERY_ALL_PACKAGES` | Install-time | Automatic | Enumerate installed apps for the app picker UI |
| Internet | `INTERNET` | Install-time | Automatic | Cloud sync (Convex) and Auth0 login |
| Read Contacts | `READ_CONTACTS` | Runtime | When adding accountability partner | Contact picker for accountability partners |

## Critical vs Non-Critical

`PermissionManager.checkAll()` classifies permissions:

- **Critical** (`criticalGranted`): Usage Stats + Overlay + Accessibility — blocking cannot work without all three
- **Non-critical**: Notifications, Battery Optimization — blocking still works but may be unreliable or silent

## Notes

- No new permissions are needed for scheduled sessions — they reuse the same blocking mechanism as manual sessions.
- `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, and Accessibility Service each require navigating to separate system settings screens.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` shows a one-tap system dialog.
- `POST_NOTIFICATIONS` is a runtime permission only on Android 13+ (API 33); on older versions it's granted automatically.
- If the user **force-stops** the app from Settings, all alarms are cancelled and the boot receiver won't fire until the app is opened again. This is an Android OS limitation no app can bypass.
