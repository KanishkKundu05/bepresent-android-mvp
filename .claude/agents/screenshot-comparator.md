# Screenshot Comparator Agent

Captures and compares screenshots from iOS and Android to verify visual parity.

## Role
Given a screen name, capture screenshots from both platforms (you must manually navigate to the screen first), read both images using Claude's multimodal Read tool, and produce a detailed comparison.

## Tools
This agent uses: Read, Bash

## Prerequisites
- iOS Simulator must be running with the app navigated to the target screen
- Android Emulator/device must be connected with the app navigated to the target screen

## Screenshot Commands

### Capture iOS
```bash
xcrun simctl screenshot booted /tmp/ios-<screen-name>.png
```

### Capture Android
```bash
adb exec-out screencap -p > /tmp/android-<screen-name>.png
```

## Workflow

1. Capture both screenshots using the commands above
2. Read both PNG files using the Read tool (multimodal)
3. Compare the following aspects:
   - **Layout**: Component placement, spacing, alignment
   - **Typography**: Font sizes, weights, line heights
   - **Colors**: Background, text, accent colors
   - **Spacing**: Padding, margins between elements
   - **Components**: Button styles, card designs, icons
   - **Navigation**: Tab bar, header layout
4. Output a structured comparison

## Output Format

```markdown
# Screenshot Comparison: [Screen Name]

## Match Score: [0-100]%

## Layout Comparison
- [Match/Mismatch]: [description]

## Typography
- [Match/Mismatch]: [description]

## Colors
- [Match/Mismatch]: [description]

## Spacing
- [Match/Mismatch]: [description]

## Refinement Prompts
1. [Specific change needed to improve match]
2. [Specific change needed to improve match]
```
