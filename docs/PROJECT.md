# Project record

## Objective

Provide a platform-neutral rule engine for validating Android semantics trees, reporting accessibility issues with WCAG 2.2 success criterion references.

## Scope

- In scope: The `A11yChecker` logic, rule evaluation over `SemanticsNode` hierarchies, and deterministic report generation.
- Out of scope: Direct integration with the Android SDK (Espresso/Compose test adapters), screen reader traversal simulation, live region/heading checks, color contrast evaluation of non-text image content, and automatic accessibility remediation.

## Timeline

| Date | Event |
|---|---|
| 2026 | Repository created. |

## Effort metrics

| Metric | Value |
|---|---|
| Tokens (input / output) | 2483718 / 33581 |
| Hours (human) | pending measurement |
| Cost (USD) | 0.8748 |

## Factory run u944 (measured from API usage)

| Date (UTC) | Phase | Model | Batch | Input | Output | Cache write | Cache read | Cost (USD) |
|---|---|---|---|---|---|---|---|---|
| 2026-11-17 | generation | google/gemini-4-argon | yes | 6539 | 15472 | 0 | 0 | 0.3356 |
| 2026-11-17 | fix-1 | google/gemini-3.8-flash | no | 146204 | 8835 | 0 | 955410 | 0.2144 |
| 2026-11-17 | fix-2 | google/gemini-3.8-flash | no | 130419 | 6652 | 0 | 779924 | 0.1813 |
| 2026-11-17 | fix-3 | google/gemini-3.8-flash | no | 22667 | 170 | 0 | 24200 | 0.0195 |
| 2026-11-17 | audit | google/gemini-3.8-flash | no | 122795 | 1224 | 0 | 276364 | 0.1174 |
| 2026-11-17 | docs | google/gemini-3.1-flash-lite | no | 19196 | 1228 | 0 | 0 | 0.0066 |

Total: $0.8748. Prices: vertex line, as measured by its adapter. Providers serve the current version of each model id, so the date tells which version answered; versions, launch stages and price sources per line are in the factory's model register (docs/MODELS.md).
