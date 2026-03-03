# Screenshot Compare

Captures and compares screenshots from iOS Simulator and Android emulator.

## Usage
`/screenshot-compare <screen-name>`

## Prerequisites
- Both iOS Simulator and Android emulator must be running
- Navigate BOTH apps to the target screen before running this skill

## Instructions

1. **Capture iOS screenshot**:
   ```bash
   xcrun simctl screenshot booted /tmp/ios-<screen-name>.png
   ```

2. **Capture Android screenshot**:
   ```bash
   adb exec-out screencap -p > /tmp/android-<screen-name>.png
   ```

3. **Read both screenshots** using the Read tool (it supports image files)

4. **Compare** the following aspects:
   - **Layout**: Component positions, alignment, hierarchy
   - **Typography**: Font sizes, weights, colors
   - **Colors**: Backgrounds, borders, accents, gradients
   - **Spacing**: Padding, margins, gaps
   - **Components**: Buttons, cards, inputs, icons
   - **Navigation**: Tab bars, headers, back buttons
   - **Content**: Text labels, placeholder values

5. **Output comparison report**:

```markdown
# Screenshot Comparison: <screen-name>

## Overall Match Score: [0-100]%

### What Matches Well
- [Aspect that looks correct]

### Differences Found
| Aspect | iOS | Android | Severity |
|---|---|---|---|
| [Item] | [iOS value] | [Android value] | High/Med/Low |

### Refinement Prompts
Specific, actionable changes to improve the Android UI to match iOS:
1. [Change description with specific values]
2. [Change description with specific values]
```
