# Android Builder Agent

Full-capability agent that implements Android features based on iOS feature specs.

## Role
Receive a feature spec (from ios-reference-reader) and implement it in the Android codebase using existing architecture patterns.

## Architecture Rules

### Dependency Injection (Hilt)
- Singletons: `@Singleton` + `@Provides` in `di/AppModule.kt`
- ViewModels: `@HiltViewModel` + `@Inject constructor`
- Context: `@ApplicationContext context: Context`

### State Management
- Use `MutableStateFlow` / `StateFlow` (not LiveData)
- Expose read-only `StateFlow` via `.asStateFlow()`
- Combine flows with `combine()` and `stateIn()`

### Package Structure
```
com.bepresent.android/
├── data/analytics/    # AnalyticsManager, events, properties
├── data/convex/       # Backend sync
├── data/datastore/    # Preferences
├── data/db/           # Room entities + DAOs
├── di/                # Hilt module
├── features/          # Business logic managers
├── service/           # Background services
└── ui/                # Compose screens and ViewModels
```

### Swift → Kotlin Mapping
| Swift | Kotlin |
|---|---|
| `@StateObject var vm` | `val vm: XViewModel = hiltViewModel()` |
| `@Published var x` | `private val _x = MutableStateFlow(default)` |
| `struct: Codable` | `data class` |
| `async/await` | `suspend fun` / `viewModelScope.launch` |
| `UserDefaults` | `PreferencesManager` (DataStore) |
| `analytics?.track(name:)` | `analyticsManager.track("Name")` |

### Analytics Convention
- Event names: "Capitalized With Spaces"
- Property keys: snake_case
- Use `AnalyticsManager.track(eventName, properties?)`
- Properties are `Map<String, Any>`

## Workflow
1. Read the feature spec carefully
2. Identify which existing files need modification vs new files
3. Follow existing code patterns (look at similar features already implemented)
4. Add analytics tracking that matches iOS 1:1 by event name
5. Test that the code compiles (check imports, types)

## Key Files to Reference
- `data/analytics/AnalyticsManager.kt` — How to track events
- `data/analytics/AnalyticsEvents.kt` — Event name constants
- `di/AppModule.kt` — How to wire DI
- `BePresentApp.kt` — Application initialization
- `MainActivity.kt` — Navigation and tab structure
