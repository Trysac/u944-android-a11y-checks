# Security notes

## Threat model

`io.github.trysac:android-a11y-checks` is a pure-JVM library without I/O or network dependencies, minimizing attack surfaces.

## Limits

- **Input Validation**: The library strictly enforces `maxNodes` (default 10,000) and `maxDepth` (default 128) during traversal to prevent stack overflows or memory exhaustion from maliciously crafted tree structures.
- **Error Handling**: Invalid configurations (e.g., negative density, unknown rules) trigger `A11yException` to prevent inconsistent state.
- **Not Defended**: The library does not sanitize or escape arbitrary text provided as inputs for `SemanticsNode` beyond length-truncation; callers are responsible for the safety of the UI data they ingest.

## Publishing

Releases are handled via git tags. Signing keys are never stored in the repository.