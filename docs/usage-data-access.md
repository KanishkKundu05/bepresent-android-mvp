# Usage Data Access

What data BePresent collects from Android's `UsageStatsManager`, how it's stored, and the compliance requirements.

## Permissions

| Permission | Type | What it enables |
|---|---|---|
| `PACKAGE_USAGE_STATS` | Special (Settings > Usage access) | Query app usage stats and events via `UsageStatsManager` |

This is a **special permission** — the user must manually grant it via system Settings. It does not require a Play Console declaration form, but the app must justify its use in the store listing and Data Safety form.

## Data Available via UsageStatsManager

### `queryUsageStats(interval, beginTime, endTime)`

Returns per-package aggregated stats for each interval bucket.

| Interval | Constant | Bucket size | Practical lookback |
|---|---|---|---|
| Daily | `INTERVAL_DAILY` | 1 day | ~7-10 days |
| Weekly | `INTERVAL_WEEKLY` | 1 week | ~4 weeks |
| Monthly | `INTERVAL_MONTHLY` | 1 month | ~6 months |
| Yearly | `INTERVAL_YEARLY` | 1 year | ~2 years |

**Fields per `UsageStats` object:**
- `packageName` — app package name
- `totalTimeInForeground` — cumulative foreground time (ms) in the interval
- `firstTimeStamp` / `lastTimeStamp` — interval boundaries
- `lastTimeUsed` — last time the app was in foreground

**Limitations:**
- Data is stored on-device only; factory reset clears everything
- Stats are aggregated per interval bucket, not per-session
- `totalTimeInForeground` can include brief focus switches (sub-second)
- Some OEMs aggressively purge data — lookback may be shorter than documented

### `queryEvents(beginTime, endTime)`

Returns a stream of individual usage events.

**Key event types:**
| Event type | Constant | Meaning |
|---|---|---|
| Move to foreground | `ACTIVITY_RESUMED` (2) | App activity came to foreground |
| Move to background | `ACTIVITY_PAUSED` (23) | App activity went to background |
| Configuration change | `CONFIGURATION_CHANGE` (5) | Device config changed (rotation, etc.) |
| Screen interactive | `SCREEN_INTERACTIVE` (15) | Screen turned on |
| Screen non-interactive | `SCREEN_NON_INTERACTIVE` (16) | Screen turned off |

**Limitations:**
- Event history is typically limited to **3-5 days** (varies by OEM/Android version)
- Events don't include which specific Activity — only the package
- No duration field — must be computed from RESUMED→PAUSED pairs
- Event stream can be very large for heavy phone users

### `queryAndAggregateUsageStats(beginTime, endTime)`

Returns a single aggregated `UsageStats` per package across the entire time range. Useful for "total screen time over past week" without bucketing.

## What BePresent Collects

### Currently (real-time)
- **Total screen time today** — `queryUsageStats(INTERVAL_DAILY)` summed
- **Per-app screen time today** — `queryAndAggregateUsageStats()` for today
- **Current foreground app** — `queryEvents()` last 10 seconds for distraction detection
- **Top distracting app** — `queryAndAggregateUsageStats()` over 7 days, filtered to known distracting apps

### Extended (new)
- **Daily per-app usage** — `queryEvents()` over last 7 days, grouped by date + package. Counts `ACTIVITY_RESUMED` events (open count) and computes foreground time from RESUMED→PAUSED pairs.
- **Monthly per-app totals** — `queryUsageStats(INTERVAL_MONTHLY)` over last 6 months. Uses system-aggregated `totalTimeInForeground`.
- **App open events** — `queryEvents()` over last 3 days. Exact timestamps with computed durations.

## Convex Schema

Daily per-app usage is synced to the `appUsageDaily` table:

```
appUsageDaily {
  userId: Id<"users">
  date: string           // "2026-03-01"
  packageName: string    // "com.instagram.android"
  appName: string        // "Instagram"
  totalTimeMs: number    // foreground time in ms
  openCount: number      // times ACTIVITY_RESUMED fired
  syncedAt: number       // epoch ms when synced
}
```

**Indexes:**
- `by_user_date` — query all apps for a user on a given date
- `by_user_package_date` — query a specific app's history (upsert dedup key)

Monthly aggregates are computed from daily data via Convex queries, not stored separately.

## Google Play Policy Compliance

Storing UsageStatsManager data server-side is **permitted** under Google Play policy with these requirements:

1. **Prominent disclosure** — Before collection, show an in-app disclosure explaining what data is collected, why, and how it's used. Must be separate from (not buried in) the privacy policy.

2. **Affirmative consent** — User must actively opt in (e.g., toggle or button). Pre-checked boxes don't count.

3. **Data Safety form** — Declare in Play Console:
   - "App activity" → "App interactions" (collected, shared: no, purpose: app functionality/analytics)
   - Data is linked to user identity
   - Encryption in transit: yes (Convex uses HTTPS)

4. **Privacy policy** — Must describe the data collected, retention period, and how users can request deletion.

No special Play Console declaration form is required for `PACKAGE_USAGE_STATS` itself — it's granted via system Settings, not a runtime permission dialog.

### Precedent
Apps like StayFree, ActionDash, and Digital Wellbeing by Google all collect and sync usage stats with opt-in consent.
