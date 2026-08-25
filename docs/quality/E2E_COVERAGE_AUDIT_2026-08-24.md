# E2E coverage audit — 2026-08-24

## Outcome

Whip now has a machine-checked behavioral coverage contract for every first-class
capability. The register contains 27 capability groups and 108 required evidence
cells: 103 are automated and five are explicitly assigned to Android-owned manual
smoke tests. Every capability has an Android instrumentation happy path.

The final API 34 run passed all 267 instrumentation tests with no failures,
errors, or skips. Together with 240 deterministic JVM tests, the product suite
contains 507 tests.

A follow-up run on the physical Samsung SM-F976W Fold running API 37 also passed
all 267 instrumentation tests with no failures or skips in 4 minutes 7 seconds.
The first physical pass exposed four test assumptions that the emulator did not:
duplicate visible labels in expanded panes, retained cross-domain Search state,
compact-only Home text, and treating Android Back as an unconditional IME-dismiss
action. The tests now target the owning pane/action, replace retained queries,
wait for layout-independent actions, and never use Back unless an IME is known to
be open. All four focused regressions and the subsequent complete device run pass.

## Measured coverage

The final AGP/JaCoCo E2E report measured:

| Scope | Coverage |
| --- | ---: |
| Product-code lines, excluding generated Room `*_Impl.kt` | 64.74% (24,834 / 38,358) |
| Product-code branches, same scope | 37.53% (11,670 / 31,095) |
| First-class repository lines | 80.12% (4,260 / 5,317) |
| First-class screen lines | 59.67% (10,777 / 18,062) |
| Track screen lines | 45.38% (1,248 / 2,750) |
| Gym screen lines | 57.42% (2,755 / 4,798) |
| Raw instrumented application lines, including generated code | 68.57% (37,060 / 54,048) |
| Raw instrumented application branches, including generated code | 39.33% (13,372 / 33,997) |

The deterministic JVM report additionally gates domain lines at 78.10%, domain
branches at 51.70%, and core policy/settings lines at 63.16%.

These code percentages are intentionally not described as “nearly 100%.” Kotlin
data-class accessors, Compose compiler output, generated Room code, and mutually
exclusive platform branches make that claim misleading. The near-complete target
is instead the traceable product behavior: 27 of 27 first-class capability groups
have automated happy-path coverage and all 108 audit cells have an assigned test
or explicit platform smoke owner.

## Gaps closed in this pass

- Added real-UI Track creation, typed Entry creation, persistence, and recreation.
- Added real-UI exercise creation, workout/set logging, completion, persistence,
  and History verification.
- Traversed every visible first-class destination, including all Task, Habit,
  Goal, Track, Gym, Search, Review, and Settings routes.
- Exercised all Settings categories and verified that each owns its expected
  controls.
- Added Track projection ordering coverage and fixed a real race where the Track
  row could be emitted before its required Fields.
- Added stable semantics for Track Entry, workout-name, and Review-filter inputs
  so tests use the same accessible controls as users.
- Added a checked coverage register that fails when a capability disappears, an
  evidence column is blank, or a referenced test is renamed or removed.
- Added local JVM and complete-emulator coverage commands plus audited
  non-regression floors near the measured baseline.

## Deliberately manual platform surfaces

The five remaining manual cells are operating-system UI rather than untested Whip
business logic:

1. notification permission/channel/lock-screen behavior;
2. Android's document-provider picker;
3. signed release upgrade on an installed app;
4. launcher widget placement/configuration; and
5. the Health Connect system permission picker.

Physical folding transitions also remain part of the release acceptance matrix;
synthetic separating, flat, tabletop, RTL, large-text, and pane-state behavior is
automated. The manual checks do not replace automated cause/effect coverage behind
those system surfaces.

## Enforced release checks

Run deterministic coverage and the normal local gate:

```bash
scripts/coverage
scripts/check
```

Run the full application E2E report only on a disposable API 34+ emulator:

```bash
ANDROID_SERIAL=emulator-5554 scripts/coverage --emulator
```

The coverage gate rejects a physical device and fails if product, repository,
screen, Track, Gym, domain, or core coverage falls below the audited floors. The
traceability contract is enforced by `E2ECoverageContractTest`.
