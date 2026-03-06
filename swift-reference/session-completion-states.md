# Blocking Session — States, Completion Flow & Reward Screens

## Session State Machine

### States (shared between iOS & Android)

| State | Value | Description |
|---|---|---|
| Idle | `"idle"` | Session created but not started |
| Active | `"active"` | Timer running, apps blocked |
| Goal Reached | `"goalReached"` | Timer hit zero — user sees "Session Complete!" UI but session is still live |
| Completed | `"completed"` | User tapped "Finish and Unblock" — rewards granted, session ended |
| Gave Up | `"gaveUp"` | User quit early (only allowed if not beast mode, after 10s) |
| Canceled | `"canceled"` | User canceled within first 10 seconds |

### Transition Graph

```
idle ──start──> active ──goalReached──> goalReached ──complete──> completed
                  │
                  ├──cancel──> canceled   (only within first 10s)
                  │
                  └──giveUp──> gaveUp    (only if !beastMode)
```

### Key Rules
- `goalReached` is automatic (alarm fires when timer expires)
- `completed` requires explicit user action ("Finish and Unblock" button)
- Rewards are ONLY granted on the `goalReached -> completed` transition (`rewardsEligible = true`)
- `gaveUp` and `canceled` get 0 XP / 0 coins

---

## Android Implementation

### State Constants
**File:** `app/src/main/java/com/bepresent/android/data/db/PresentSession.kt:22-28`

```kotlin
const val STATE_IDLE = "idle"
const val STATE_ACTIVE = "active"
const val STATE_GOAL_REACHED = "goalReached"
const val STATE_COMPLETED = "completed"
const val STATE_GAVE_UP = "gaveUp"
const val STATE_CANCELED = "canceled"
```

### State Machine
**File:** `app/src/main/java/com/bepresent/android/features/sessions/SessionStateMachine.kt`

- `Transition` data class (line 8): carries `rewardsEligible`, `clearActiveSession`, `syncAfter`, etc.
- `complete()` (line 91): `STATE_GOAL_REACHED -> STATE_COMPLETED` with `rewardsEligible = true`
- `calculateRewards()` (line 108): returns `Pair<Int, Int>` (xp, coins)

### Session Manager — Reward Granting
**File:** `app/src/main/java/com/bepresent/android/features/sessions/SessionManager.kt`

- `applyTransition()` (line 126): applies state change, persists to Room, logs analytics
- Line 140-144: calculates rewards if `transition.rewardsEligible`
- Line 150-151: writes `earnedXp` / `earnedCoins` to the session entity
- Line 190-192: calls `preferencesManager.addXpAndCoins(xp, coins)` to update totals

### UI Sub-States
**File:** `app/src/main/java/com/bepresent/android/ui/homev2/components/ActiveSessionCard.kt:49-53`

```kotlin
enum class ActiveSessionSubState {
    Active,       // Timer counting down, break/give-up controls visible
    BreakRunning, // Apps temporarily unlocked, break timer showing
    Completed     // "Session Complete!" title, burst animation, no controls
}
```

### ViewModel — Timer & Completion Detection
**File:** `app/src/main/java/com/bepresent/android/ui/homev2/HomeV2ViewModel.kt`

- `startSessionTimerIfNeeded()` (line 191): polls every 1s, maps DB state to `ActiveSessionSubState`
- Line 205-209: maps both `STATE_GOAL_REACHED` and `STATE_COMPLETED` to `ActiveSessionSubState.Completed`
- `completeSession()` (line 237): calls `sessionManager.complete()` then `resetToIdle()`

### Active Session Card — Completion UI
**File:** `app/src/main/java/com/bepresent/android/ui/homev2/components/ActiveSessionCard.kt`

When `subState == Completed`:
- **Drifting clouds hidden** (line 86)
- **Burst animation shown** (lines 102-123): 200dp rotating image, 30s infinite rotation at 0.6 alpha
- **Brick icon** (lines 125-129): 126dp session-brick centered over burst
- **Title**: "Session Complete!" (line 137)
- **Progress bar + XP display** still visible (lines 206-235)
- **No break/give-up controls** (line 277-279, 283-298)

### What's NOT Yet Implemented (Android)
- **XP Reward Popup** — iOS has `XPRewardPopup.swift` (animated counter + haptics + auto-dismiss)
- **ClaimXP full-screen** — iOS has `ClaimXPView.swift` (full-screen cover with stats cards + "Claim XP" button)
- **"Finish and Unblock" button** — iOS shows a green success button in `.complete` state; Android currently auto-calls `completeSession()` / `resetToIdle()` without this explicit step

---

## iOS Implementation (Reference)

### Session Actions
**File:** `ScreentoxMonitorExtension/Managers/SharedPresentSessionManager.swift:1029-1036`

```swift
enum PresentSessionAction: String, Codable {
    case started, canceled, gaveUp, completed, logOut, unknown
}
```

### Active Session States
**File:** `Screens/HomeV2/Components/ActiveSession/ActiveSessionV2ViewModel.swift:14-17`

```swift
enum ActiveSessionState {
    case active    // Timer running
    case complete  // Goal reached, "Finish and Unblock" button shown
}
```

### Completion Detection
**File:** `ActiveSessionV2ViewModel.swift:292-306`

The `SessionTimer` publishes `completed = true` when `timeElapsed >= goal`. The ViewModel sets `self.completed = value`, which:
- Changes `sessionTitle` to "Session Complete!" (line 52-55)
- Shows burst animation + hides drifting clouds (ActiveSessionV2View lines 30-52)
- Swaps the bottom button from "Give Up" to "Finish and Unblock" (ActiveSessionV2View lines 326-333)

### End Session Flow
**File:** `ActiveSessionV2ViewModel.swift:124-173`

`endSession(state: .complete)`:
1. Builds `PresentSessionActionInfo` with `action: .completed`, `pointsGained`, `coinsGained`, `extraTime`
2. Sets `AppState.shared.showXPPopup = true` (line 162)
3. Stops the session timer
4. After 0.5s delay, calls `manager.endSession(timeElapsed:action:.completed, extraTime:)`
5. Calls `AppState.shared.getScore()` to refresh leaderboard

### Reward Calculation (iOS)
**File:** `SharedPresentSessionManager.swift:674-725`

| Goal Duration | XP | Coins |
|---|---|---|
| <= 15 min | 3 | 1 |
| <= 30 min | 5 | 2 |
| <= 45 min | 8 | 4 |
| <= 60 min | 10 | 5 |
| <= 75 min | 13 | 7 |
| <= 90 min | 15 | 8 |
| <= 105 min | 20 | 9 |
| > 105 min | 25 | 10 |

### Reward Calculation (Android)
**File:** `SessionStateMachine.kt:108-118`

| Goal Duration | XP | Coins |
|---|---|---|
| <= 15 min | 3 | 3 |
| <= 30 min | 5 | 5 |
| <= 45 min | 8 | 8 |
| <= 60 min | 10 | 10 |
| <= 90 min | 15 | 15 |
| > 90 min | 25 | 25 |

> **Note:** Android uses symmetric XP/coins and has fewer tiers (6 vs 8). iOS has asymmetric values (XP > coins).

---

## Post-Completion Screens (iOS — not yet ported)

### 1. XP Reward Popup (`XPRewardPopup.swift`)
**File:** `Features/Misc/XPRewardPopup.swift`

A floating overlay shown immediately when `showXPPopup = true`:
- Gradient rounded rectangle (brand colors)
- Pulsing bolt icon with glow
- Animated XP counter: counts from 1 to final value over 0.8s
- Haptics: medium impact on appear, soft every 2 increments, success at 0.35s
- Auto-dismisses after 2.5s with fade-out
- Displayed as overlay in `ContentView.swift:98-105`

### 2. Claim XP Full-Screen (`ClaimXPView.swift`)
**File:** `Screens/HomeV2/Components/ClaimXP/ClaimXPView.swift`

Presented as `.fullScreenCover` from `HomeV2View.swift:37-49` after the popup:
- **Background:** `claim-xp-bg` image
- **Sequenced animation** (managed by `ClaimXPViewModel.animateIn()`):
  - 0.0s: Bolt icon pops in (spring animation)
  - 0.3s: "Session complete!" + "Great job being present!" text slides up
  - 0.8s: **Total XP** stat card appears + count animation (0 -> points, 0.5s)
  - 1.6s: **Time Blocked** stat card appears + count animation
  - 2.4s: **Leaderboard Rank** stat card appears + countdown from 30th (if seen intro)
  - 3.2s: "Claim {X} XP" button slides up from bottom
- Tapping "Claim XP" either:
  - Shows `LeaderboardIntroView` (first time) then dismisses
  - Dismisses directly (returning users)
- On dismiss: `showXPPopup = false`, posts `didDismissClaimXP` notification

### Full iOS Completion Flow (sequence)

```
Timer hits 0
  -> completed = true
  -> UI: "Session Complete!" + burst + "Finish and Unblock" button

User taps "Finish and Unblock"
  -> endSession(state: .complete)
  -> AppState.showXPPopup = true  (triggers XPRewardPopup overlay)
  -> manager.endSession(action: .completed)  (grants XP/coins, syncs)
  -> XPRewardPopup auto-dismisses after 2.5s
  -> latestCompletedSession set  (triggers ClaimXPView full-screen cover)
  -> ClaimXPView animates in (icon -> text -> stat cards -> button)

User taps "Claim XP"
  -> (optional) LeaderboardIntroView if first time
  -> Dismiss -> back to idle home screen
```

---

## Files Index

| Purpose | Android | iOS |
|---|---|---|
| Session entity + states | `data/db/PresentSession.kt` | `SharedPresentSessionManager.swift` |
| State machine / transitions | `features/sessions/SessionStateMachine.kt` | `SharedPresentSessionManager.swift` |
| Session manager (reward granting) | `features/sessions/SessionManager.kt` | `PresentSessionManager.swift` |
| Active session UI | `ui/homev2/components/ActiveSessionCard.kt` | `ActiveSessionV2View.swift` |
| Active session ViewModel | `ui/homev2/HomeV2ViewModel.kt` | `ActiveSessionV2ViewModel.swift` |
| XP reward popup | *not implemented* | `Features/Misc/XPRewardPopup.swift` |
| Claim XP full-screen | *not implemented* | `Screens/HomeV2/Components/ClaimXP/ClaimXPView.swift` |
| Claim XP ViewModel | *not implemented* | `Screens/HomeV2/Components/ClaimXP/ClaimXPViewModel.swift` |
| Popup trigger (app state) | *not implemented* | `Core/Models/AppState.swift` (`showXPPopup`) |
