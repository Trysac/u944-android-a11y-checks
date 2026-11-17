# android-a11y-checks

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![CI](https://github.com/Trysac/u944-android-a11y-checks/actions/workflows/ci.yml/badge.svg)](https://github.com/Trysac/u944-android-a11y-checks/actions/workflows/ci.yml)

Accessibility rule engine for Android view and Compose semantics snapshots that reports touch target, contrast and labelling problems with WCAG 2.2 success criterion references

## Problem

Describe in two or three sentences the concrete problem this utility solves, who hits it, and what
they do today without it. Replace this paragraph when the first real feature lands.

## Install

Maven coordinates `io.github.trysac:android-a11y-checks`. Until it is published to Maven Central, build
it from a release tag through JitPack (`https://jitpack.io`), then
declare in `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.trysac:android-a11y-checks:0.1.0")
}
```

Requires Java 21 or newer. No dependencies outside the Kotlin standard library.

## Usage

```kotlin
import io.github.trysac.android_a11y_checks.Text

// normalize trims surrounding whitespace and lower-cases the text.
val name = Text.normalize("  Hello World ") // "hello world"

// Blank input is rejected explicitly instead of returning an empty string.
Text.normalize("   ") // throws BlankInputException ("input is blank")
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md) and [decisions](docs/decisions/)
- [Testing](docs/TESTING.md) and [security](docs/SECURITY.md)
- [Project record](docs/PROJECT.md) and [stack](docs/STACK.md)
- [Changelog](CHANGELOG.md) and [contributing guide](CONTRIBUTING.md)

## License

[MIT](LICENSE) (c) 2026 Miguel Gomez
