# iOS Reference Extraction

Extracts a complete feature spec from the iOS codebase.

## Usage
`/ios-reference <feature-name>`

## Instructions

Search the iOS codebase at `/Users/kanishk/Desktop/bepresent/prod/swift/Screentox/` for all files related to the given feature.

### Steps

1. **Search for the feature** across the iOS codebase:
   - Grep for the feature name in file names and content
   - Check `Features/`, `Screens/`, `Core/` directories
   - Look for related ViewModels, Views, Managers, and Models

2. **Read all related files** completely

3. **Extract analytics events**:
   - Search for `.track(name:` and `analytics?.track` in found files
   - Document every event name and its properties
   - Note which user action triggers each event

4. **Extract data models**:
   - Find structs/classes used by the feature
   - Document fields, types, and CodingKeys

5. **Extract UI details**:
   - Screen layout and component hierarchy
   - Navigation flow (how screens connect)
   - Colors, fonts, spacing tokens

6. **Output structured spec**:

```markdown
# iOS Feature Spec: [Feature Name]

## Screen Flow
1. [Screen] → [Next Screen] (user action)

## Analytics Events
| Event Name | Properties | Trigger |
|---|---|---|

## Data Models
[List of structs with fields]

## UI Components
[Layout description, colors, typography]

## Source Files
[List of iOS files read]
```

### Key iOS Directories
- `Core/Analytics/` — AnalyticsManager, event tracking
- `Core/Util/Constants.swift` — Shared constants and property structs
- `Features/` — Feature-specific code
- `Features/Onboarding/V2/` — Onboarding flow
- `Features/Sessions/` — Present session management
- `Features/AppIntentions/` — App intention management
