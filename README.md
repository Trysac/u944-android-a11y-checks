# android-a11y-checks

Android teams typically catch accessibility defects in manual audits or store reviews, as standard UI tests often ignore touch target size, contrast ratios, and labelling requirements. `android-a11y-checks` provides a platform-neutral, pure-JVM accessibility rule engine for Android View and Jetpack Compose semantics snapshots.

## Installation

```kotlin
implementation("io.github.trysac:android-a11y-checks:1.0.0")
```

## Usage

```kotlin
import io.github.trysac.android_a11y_checks.*

val root = SemanticsNode("root", Role.BUTTON, Rect(0, 0, 40, 40), clickable = true)
val checker = A11yChecker(density = 2.0f)
val report = checker.check(root)
println(report.toJson())
```

## Public API

- `A11yChecker(density: Float, rules: Set<String>, maxNodes: Int, maxDepth: Int)`: Validates configuration and executes checks.
- `A11yChecker.check(root: SemanticsNode)`: Returns `A11yReport`. Throws `A11yException` (`tree_too_large`, `too_deep`, `invalid_density`) if bounds are exceeded.
- `SemanticsNode`: Snapshot model of UI elements with properties like `role`, `boundsPx`, `text`, `clickable`, etc.
- `Contrast.ratio(fg: Int, bg: Int)`: Calculates WCAG 2.2 contrast ratio for sRGB colors.

## Why not Accessibility Test Framework for Android?

Accessibility Test Framework for Android requires `android.view.View` or `AccessibilityNodeInfo` instances, making it difficult to run in pure JVM unit tests without Robolectric or an emulator. `android-a11y-checks` uses a platform-free `SemanticsNode` snapshot, enabling fast, emulator-free accessibility assertions in standard unit tests.

## Security

See [docs/SECURITY.md](docs/SECURITY.md).