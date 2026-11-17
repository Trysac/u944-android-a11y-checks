# Architecture

## Overview

`android-a11y-checks` is a pure-JVM Kotlin library in package `io.github.trysac.android_a11y_checks` with zero external runtime dependencies. It separates UI tree capture (performed by thin Compose or Espresso test adapters in an Android project) from accessibility rule evaluation.

## Components

1. **Snapshot Model (`SemanticsNode`, `Role`, `Rect`)**: Immutable value objects representing a captured UI tree in physical pixels (`boundsPx`), scale-independent font size (`textSizeSp`), ARGB colors, and accessibility properties (`importantForAccessibility`, `mergeDescendants`).
2. **Contrast Calculator (`Contrast`)**: Implements the WCAG 2.2 relative luminance formula over 8-bit sRGB channels linearized at the `0.04045` threshold, ignoring the alpha channel.
3. **Rule Engine (`A11yChecker`)**:
   - Validates configuration (`density`, `rules`, `maxNodes`, `maxDepth`) at construction.
   - Performs an iterative pre-order traversal to enforce `maxNodes` (`tree_too_large`) and `maxDepth` (`too_deep`) before evaluating rules.
   - Skips subtrees where `importantForAccessibility == false` or `boundsPx` has non-positive width or height.
   - Evaluates the 7 rules (`touch_target`, `contrast`, `missing_label`, `image_no_label`, `editable_no_label`, `duplicate_label`, `redundant_role`) and truncates any quoted user text snippet to 40 characters and total issue messages to 200 characters.
4. **Reporting (`A11yReport`, `Issue`)**: Stores issues sorted by `(rule, path)` and serializes them deterministically via `toJson()` without third-party JSON dependencies.
