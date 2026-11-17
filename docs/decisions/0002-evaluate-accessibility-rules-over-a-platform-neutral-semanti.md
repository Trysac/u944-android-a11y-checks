# Evaluate accessibility rules over a platform-neutral semantics snapshot

Status: accepted (generated, pending human review)

## Context

Android accessibility checks in Espresso and Jetpack Compose typically require the Android SDK or AccessibilityNodeInfo at runtime, preventing fast JVM verification and omitting WCAG 2.2 criterion identifiers.

## Decision

Define an immutable, standard-library-only SemanticsNode snapshot model and evaluate the seven accessibility rules and WCAG 2.2 relative luminance contrast formulas directly on the JVM with explicit node count (10,000) and depth (128) bounds.

## Consequences

The rule engine runs deterministically in plain JVM unit tests with zero external dependencies and byte-identical JSON output, while Android UI tests only need a ~20-line adapter to convert View or Compose nodes into SemanticsNode trees.
