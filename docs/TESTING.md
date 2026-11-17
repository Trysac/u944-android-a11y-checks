# Testing

## Running

```sh
java scripts/Gradle.java test
```

## Policy

- Line coverage must be at or above 80%.
- Property-based tests (jqwik) state invariants.
- Tests assert observable effects (returned values, thrown `A11yException` types).

## Coverage

- `A11yChecksTest.kt`: Covers contrast calculation, touch target sizing based on density, seeded broken vs. fixed screen scenarios, node skipping (unimportant/zero-size), JSON serialization, and `A11yException` bounds validation.
- `A11yPropertiesTest.kt`: Covers property-based invariants for `Contrast.ratio` (symmetry/bounds) and touch target assertions.
- `TextTest.kt` & `TextPropertiesTest.kt`: Covers `Text.normalize` string processing logic and `BlankInputException` handling.