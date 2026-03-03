# Implement From iOS

End-to-end pipeline for porting an iOS feature to Android.

## Usage
`/implement-from-ios <feature-name>`

## Instructions

### Phase 1: Extract iOS Spec
Spawn an `ios-reference-reader` agent to analyze the iOS codebase and extract a complete feature spec including:
- Screen flow
- Analytics events (every event name and property)
- Data models
- UI details

### Phase 2: Generate Android Implementation Plan
Based on the iOS spec, create a detailed implementation plan:

1. **Map Swift → Kotlin/Compose** using these patterns:
   | Swift | Kotlin |
   |---|---|
   | `ObservableObject` → `@HiltViewModel` | `@Published` → `MutableStateFlow` |
   | `@State` → `remember { mutableStateOf() }` | `NavigationLink` → `navController.navigate()` |
   | `Codable struct` → `data class` | `UserDefaults` → `PreferencesManager` |

2. **Identify files to create vs modify**:
   - New ViewModels, Screens, Components
   - Existing files that need analytics calls added
   - DI wiring needed

3. **Identify parallelizable tasks**:
   - Tasks that touch different files can run in parallel
   - Tasks that share files must be sequential

### Phase 3: Implement
For each component:
1. Create/modify the Android file following existing patterns
2. Add analytics tracking that matches iOS 1:1 by event name
3. Use `AnalyticsEvents` constants (never hardcode event names)
4. Follow Hilt DI patterns for any new dependencies

### Phase 4: Verify
- Run `./gradlew assembleDebug` to check compilation
- List all iOS events and verify each has an Android equivalent
- Check that property names match (snake_case)

## Key Android Files
- `data/analytics/AnalyticsManager.kt` — Track events
- `data/analytics/AnalyticsEvents.kt` — Event name constants
- `data/analytics/AnalyticsProperties.kt` — Property data classes
- `di/AppModule.kt` — DI wiring
- `BePresentApp.kt` — App initialization
- `MainActivity.kt` — Navigation structure
