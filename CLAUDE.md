# BePresent Android — Claude Code Context

## Project Overview
BePresent is a digital wellbeing app for Android, ported from a production iOS app. Core features: App Intentions (per-app daily open limits), Blocking Sessions (timed focus sessions), Leaderboards, Accountability Partners, and Onboarding with paywall.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Database**: Room (`bepresent.db`)
- **Preferences**: DataStore (`bepresent_prefs`)
- **Analytics**: Mixpanel (`com.mixpanel.android:mixpanel-android`)
- **Backend**: Convex (via `dev.convex:android-convexmobile`)
- **Auth**: Auth0
- **Payments**: Stripe
- **Background**: Foreground Service (monitoring), WorkManager (daily reset), AlarmManager (timers)
- **Min SDK**: 26 (Android 8) / **Target SDK**: 34 (Android 14)
- **Build**: AGP 8.2.2, Kotlin 1.9.22, KSP, Gradle 8.5

## Architecture
Single-module app. Single Activity (`MainActivity`) hosting Compose navigation. `BlockedAppActivity` runs in a separate task for shield screens.

### Key Directories
```
app/src/main/java/com/bepresent/android/
├── data/
│   ├── analytics/    # AnalyticsManager, event constants, property models
│   ├── convex/       # ConvexManager, SyncManager, SyncWorker
│   ├── datastore/    # PreferencesManager (XP, coins, streaks, flags)
│   ├── db/           # Room entities + DAOs (AppIntention, PresentSession, ScheduledSession)
│   ├── subscription/ # SubscriptionManager (Stripe)
│   └── usage/        # UsageStatsRepository (screen time, foreground detection)
├── di/               # Hilt AppModule
├── features/
│   ├── blocking/     # BlockedAppActivity + ShieldScreen (3 variants)
│   ├── intentions/   # IntentionManager + DailyResetWorker
│   ├── schedules/    # ScheduledSessionManager
│   └── sessions/     # SessionManager + SessionStateMachine
├── service/          # MonitoringService, BootCompletedReceiver, alarm receivers
├── permissions/      # PermissionManager + OemBatteryGuide
└── ui/               # All Compose UI
    ├── auth/         # AuthViewModel
    ├── homev2/       # Home screen (main tab)
    ├── onboarding/v2/ # Onboarding flow with paywall
    ├── leaderboard/  # Leaderboard tab
    ├── screentime/   # Screen Time tab
    ├── social/       # Accountability partners tab
    ├── schedules/    # Schedules tab
    ├── profile/      # Profile screen
    └── theme/        # Color, Theme, Type tokens
```

## iOS Reference
The production iOS app lives at: `/Users/kanishk/Desktop/bepresent/prod/swift/Screentox/`

Key iOS reference files:
- `Screentox/Core/Analytics/AnalyticsManager.swift` — Mixpanel + CustomerIO tracking
- `Screentox/Core/Analytics/analytics.md` — Event naming docs
- `Screentox/Core/Analytics/LightAnalyticsManager.swift` — HTTP-based tracking for extensions
- `Screentox/Core/Util/Constants.swift` — Tokens and shared property structs

### Swift → Kotlin/Compose Pattern Mapping

| Swift / SwiftUI | Kotlin / Compose |
|---|---|
| `class ViewModel: ObservableObject` | `@HiltViewModel class ViewModel @Inject constructor(...) : ViewModel()` |
| `@StateObject var vm` | `val vm: MyViewModel = hiltViewModel()` |
| `@Published var x` | `private val _x = MutableStateFlow(...)` / `val x: StateFlow<T> = _x.asStateFlow()` |
| `@State var x` | `var x by remember { mutableStateOf(...) }` |
| `@Binding var x` | Function parameter `x: T, onXChange: (T) -> Unit` |
| `@EnvironmentObject` | Hilt injection via `@Inject constructor(...)` |
| `UserDefaults / @AppStorage` | `DataStore<Preferences>` via `PreferencesManager` |
| `NavigationStack / NavigationLink` | `NavHost` + `NavController` + `composable()` routes |
| `VStack { }` | `Column { }` |
| `HStack { }` | `Row { }` |
| `ZStack { }` | `Box { }` |
| `.sheet(isPresented:)` | `ModalBottomSheet` or dialog composable |
| `Color("name")` | `HomeV2Tokens.ColorName` or `MaterialTheme.colorScheme.x` |
| `Timer.publish(every:)` | `LaunchedEffect { while(true) { delay(interval) } }` |
| `Codable struct` | `data class` with `@Serializable` or manual JSON |
| `async/await` | `suspend fun` / `viewModelScope.launch { }` |
| `@Singleton (Swinject)` | `@Singleton` + `@Provides` in `AppModule` (Hilt) |

## Analytics Convention
- **Event names**: "Capitalized With Spaces" (e.g., "Started Present Session")
- **Property keys**: snake_case (e.g., `previous_version`, `goal_duration_minutes`)
- **Tracking call**: `analyticsManager.track("Event Name")` or `analyticsManager.track("Event Name", mapOf(...))`
- **Profile updates**: Certain events auto-update Mixpanel user properties ("Last Active", "Last Session", "Last App Open")

## Screenshot Capture Commands
- **iOS**: `xcrun simctl screenshot booted /tmp/ios-<screen-name>.png`
- **Android**: `adb exec-out screencap -p > /tmp/android-<screen-name>.png`

## Conventions
- Commit messages: lowercase, concise, imperative
- No AccessibilityService (UsageStats polling only for MVP)
- Soft enforcement: users can always "Open Anyway" (streak breaks)
- Session priority over intentions when both block the same app

## Spec Documents
- `planning/mvp-single-screen.md` — Full MVP specification (source of truth)
- `planning/android-implementation-guide.md` — API code samples
- `planning/android-critical-considerations.md` — Known limitations and edge cases
- `swift-reference/` — iOS porting docs + screenshots + component breakdowns
