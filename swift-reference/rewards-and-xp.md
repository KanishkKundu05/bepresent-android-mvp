# Rewards & XP System -- iOS Reference Spec

## Overview

BePresent uses a dual-currency system:
- **XP (points)** -- earned by completing blocking sessions; determines weekly leaderboard rank
- **Coins** -- earned alongside XP; spent in the Rewards shop to unlock partner offers

In the current iOS production code, coins are set equal to XP (`coinsFromPresentSessionGoal` delegates to `pointsFromPresentSessionGoal`). An earlier, commented-out implementation had asymmetric coin values (see "Historical Coin Tiers" below).

---

## XP / Coins Reward Tiers for Manual Blocking Sessions

Rewards are calculated at session **creation** time based on the goal duration and stored on the session object (`points`, `coins` fields). They are granted when the session completes.

### Active XP Tiers (production)

Source: `pointsFromPresentSessionGoal(goal:)` in `SharedPresentSessionManager.swift` (line 674)

| Goal Duration | XP Earned |
|---|---|
| <= 15 min | 3 |
| <= 30 min | 5 |
| <= 45 min | 8 |
| <= 60 min | 10 |
| <= 75 min (1:15) | 13 |
| <= 90 min (1:30) | 15 |
| <= 105 min (1:45) | 20 |
| > 105 min (2:00+) | 25 |

### Active Coin Tiers (production)

Source: `coinsFromPresentSessionGoal(goal:)` in `SharedPresentSessionManager.swift` (line 695)

Currently returns the same value as XP (line 733: `return pointsFromPresentSessionGoal(goal: goal)`).

| Goal Duration | Coins Earned (current) |
|---|---|
| <= 15 min | 3 |
| <= 30 min | 5 |
| <= 45 min | 8 |
| <= 60 min | 10 |
| <= 75 min (1:15) | 13 |
| <= 90 min (1:30) | 15 |
| <= 105 min (1:45) | 20 |
| > 105 min (2:00+) | 25 |

### Historical Coin Tiers (commented out in iOS)

These were the original asymmetric coin values, now commented out:

| Goal Duration | Coins Earned (historical) |
|---|---|
| < 15 min | 1 |
| < 30 min | 2 |
| < 45 min | 3 |
| < 60 min | 4 |
| < 75 min | 5 |
| < 90 min | 6 |
| < 105 min | 7 |
| < 120 min | 8 |
| >= 120 min | 10 |

---

## When Rewards Are Granted

### State transition flow

1. **Session created** -- `points` and `coins` are calculated from goal duration and stored on the `PresentSession` object via `createSession()`.
2. **Session started** -- A `PresentSessionAction` with action `.started` is recorded with `pointsGained: 0, coinsGained: 0`.
3. **Goal reached** -- Timer fires; intermediate state (no reward action yet).
4. **Session completed** -- The `.completed` action is recorded with `pointsGained: points, coinsGained: coins` (the values stored at creation).
   - `SharedUserState.addCoins(coins:)` is called with the session's coin value to update the user's total.
   - `AppState.getScore()` recalculates weekly XP from the database.
5. **Session gave up / canceled** -- `pointsGained: 0, coinsGained: 0` is recorded. No rewards.

Key code paths:
- **Main app**: `PresentSessionManager.endSession()` records the action and calls `AppState.shared.getScore()`.
- **Monitor extension (scheduled sessions)**: `SharedPresentSessionManager.endSession()` saves the action to UserDefaults and calls `SharedUserState.singleton.addCoins(coins:)`.
- **Tutorial sessions**: `endSessionTutorial()` records a `.completed` action with `pointsGained: points, coinsGained: points`.

---

## How Totals Are Stored and Updated

### XP (Weekly Points)

- XP is **not** stored as a running total. It is computed on demand.
- `DataController.getWeeklyPoints()` sums `pointsGained` from all `PresentSessionActions` since the most recent Monday.
- Used for leaderboard ranking and the home screen weekly XP display.

### Coins

- Stored as a running total in `UserDefaults` under a user-scoped key (`coins`).
- Managed by `SharedUserState.getCoins()` / `setCoins()` / `addCoins()`.
- `UserState.addCoins()` (subclass) also pushes the updated total to the server via `Network.shared.updateUserCoins()`.
- Coins can be subtracted when purchasing rewards (`UserState.subtractCoins()`).
- The server can also grant coins asynchronously (`getGrantedUserCoins()`).
- Display convention: `userState.coins / 100` in UI (coins are stored in "centi-points" internally; 100 XP = 1 display point).

---

## Other Sources of XP / Coins

### Daily Quests

The Daily Quest card has three tasks:
1. **Yesterday's Review** -- View the daily screen time report.
2. **Tip of the Day** -- View the daily tip.
3. **Present Session** -- Complete at least one blocking session.

These are tracked as completion flags only (boolean). There is **no separate XP/coin reward** for completing daily quests -- the session task gives XP/coins through the normal session reward flow.

### Streaks

Streaks are tracked via `StreakReport` and relate to screen time goals (staying under a daily screen time threshold). Streaks do **not** directly grant XP or coins. They are a separate engagement metric displayed on the home screen.

### Leaderboard XP

Leaderboard ranking uses the same weekly XP sum from `getWeeklyPoints()`. There is no additional XP source from leaderboard participation itself.

### Screen Time Score

`getPointsFromThreshold()` in `ScreenTimeThresholds.swift` calculates a 0-100 "score" based on daily screen time thresholds hit. This score is **not** XP -- it is a separate metric used for the legacy scoring system and streak calculations.

### Granted Coins (Server-Side)

The server can grant bonus coins to users. These are fetched via `getGrantedUserCoins()` and added to the local coin total. This is an admin/operational mechanism, not a regular gameplay reward.

---

## Android Implementation Notes

- `SessionStateMachine.calculateRewards(goalDurationMinutes)` returns `Pair<Int, Int>` where `first` = XP, `second` = coins.
- The Android implementation should match the iOS XP tiers exactly (8 brackets, not 6).
- The `rewardsEligible` flag on the `complete()` transition indicates when to grant rewards.
- Coins in the Android `PreferencesManager` should be stored as raw values (same scale as XP, not divided by 100).
