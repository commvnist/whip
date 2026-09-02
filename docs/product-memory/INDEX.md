# Whip durable product memory

Last reconciled: 2026-09-02

This is the canonical cross-session entry point for Whip product and engineering context. It indexes durable conclusions and evidence; current code and reproducible behavior remain authoritative.

## Read order

1. Read this file completely.
2. Search [user feedback](USER_FEEDBACK.md), [findings](FINDINGS.md), [decisions](DECISIONS.md), [implementation history](IMPLEMENTATION_LOG.md), and [verification](VERIFICATION.md) for the active feature and identifiers.
3. Read linked detailed audits and inspect the current implementation before relying on a recorded claim.
4. Use [the maximum-quality goal](MAXIMUM_QUALITY_GOAL.md) for an exhaustive product iteration.

## Current product snapshot

- Latest device-verified release: **0.3.34 (version code 40)**, installed 2026-08-31 with existing data preserved. See `VER-20260831-002`.
- Signed release APK SHA-256: `7ddc4bfb209fef541530602420d8daa316f2e74c9b3d19f2c433f02624d7edfe`.
- Released Gym/5/3/1 implementation and audit commit: `5fc98dd` on `origin/main`.
- Declared test-source baseline: **1,410 product tests: 547 JVM and 863 Android**. The complete current baseline was freshly executed for `VER-20260902-002`; the most recent separate continuous E2E coverage campaign remains `VER-20260901-022`.
- The latest Gym release includes arbitrary-lift 5/3/1 creation, explicit/derived Training Max controls inside and outside 5/3/1, performance-informed cycle review, workout-only exercise addition, contextual routine return, adaptive routine editing, timer-boundary correction, and additive Joker behavior.
- The verified post-release Gym candidate additionally has exact quick-set/finish and structure boundaries, immutable required Main-work outcomes, retained retired/tombstone history, transactionally coherent active projections, an explicit responsive Arrange editor with value-preserving Undo, recoverable History Copy identities, request-owned lifecycle results, and durable PR/Link/rest-timer reconciliation. See `VER-20260901-018` and `VER-20260901-020`; it is not yet a signed physical-device release.
- The latest verified candidate also makes every typed Settings value an explicit, bounded, request-owned durable edit with strict parsing, commit-failure rollback/retry, lifecycle/conflict protection, and IME/large-text reachability. See `FND-20260831-013`, `DEC-20260901-024`, `IMP-20260901-017`, and `VER-20260901-019`. It is not yet a signed physical-device release.
- Health Connect, Custom Units, backup/restore, and Reset Whip are now independently verified against atomic mirror reconciliation, lifecycle-owned authorship, domain-specific unit semantics, private-versus-portable recovery state, proof-based legacy repair, and an application-wide exclusive maintenance gate. See `FND-20260831-020`, `FND-20260901-027`, `FND-20260901-028`, `IMP-20260901-019`, and `VER-20260901-021`. This remains an emulator-verified candidate, not yet a signed physical-device release.
- Android Share-to-Task and widget Task creation are now bounded before saved state, delivered through an exact saveable FIFO, and prevented from silently replacing an open draft. Unicode-safe shortening, request-owned date/Area defaults, deliberate replacement/defer behavior, and a durable counted queue-overflow acknowledgment are independently accepted and emulator-verified in `IMP-20260901-020` / `VER-20260901-022`. This is not yet a signed physical-device release.
- Habit timers now have a durable request-owned session ledger, canonical Duration conversion, monotonic live display, explicit reboot/restore review, portable/private backup semantics, stable widget actions, and accessible recovery UI. See `FND-20260902-001`, `DEC-20260902-002`, `IMP-20260902-002`, and `VER-20260902-002`. This is an emulator- and release-build-verified candidate, not yet a physical-device release.
- Detailed Gym evidence lives in [the 5/3/1 product audit](../GYM_531_PRODUCT_AUDIT_2026-08-31.md) and [testing inventory](../testing.md).

## Active direction

- `FB-20260831-012`: the maximum-quality whole-product backlog remains the product direction. Its initial multi-agent inventory and synthesis are preserved as historical evidence in `VER-20260831-005` and `../WHOLE_PRODUCT_MAXIMUM_QUALITY_AUDIT_2026-08-31.md`. Per `FB-20260902-001` / `DEC-20260902-001`, ongoing work now uses a conventional single-developer loop with no subagents, simulated panels, recursive audits, formal debates, or repeated approval rounds unless the user explicitly requests them.
- `FND-20260831-007` P0 restore recovery, `FND-20260831-008` reminder-delivery integrity, `FND-20260831-009` shared live calendar/time-zone consistency, `FND-20260831-010` Task/Habit/Goal authored-save integrity, `FND-20260831-013` typed Settings integrity, `FND-20260831-014` bounded external Task capture, `FND-20260831-015` Goal backup history, `FND-20260831-016` Goal historical IA, `FND-20260831-020` batch-atomic Health reconciliation, `FND-20260901-022` stale Track schema integrity, `FND-20260901-023` direct Track Entry integrity, `FND-20260901-024` CSV batch identity/recovery, `FND-20260901-025` exact Gym session outcomes, `FND-20260901-026` lightweight Gym structure exactness, `FND-20260901-028` restore/reset integrity, `FND-20260902-001` Habit timer integrity, and the Habit, Task, Goal, Track, Gym, Custom Unit, and Health subsets of `FND-20260831-019` / `FND-20260901-027` are resolved and verified. Remaining progressive Habit UX, named large-text navigation, returning Home, Track search ownership, and other secondary families follow.
- `FB-20260831-013`: keep durable project memory outside chat. The infrastructure and skill package are structurally validated; future task use will validate whether the protocol needs adjustment.
- `FB-20260831-014`: commit and push each coherent verified chunk for easier tracking and reverts. The workflow rule is implemented and structurally validated in the skill, workspace instructions, memory schema, and whole-product goal.
- Treat subjective fixes in release 0.3.34 as awaiting continued real-user validation even where automated and device checks pass.

## Highest-priority unresolved verification

- Continue the remaining risk-proportionate secondary families: progressive Habit correctness/UX, named large-text navigation, returning Home, Track search ownership, and whole-product accessibility/platform matrices. Preserve the exact Health/Custom Unit/restore/reset contracts from `FND-20260831-020`, `FND-20260901-027`, and `FND-20260901-028`; the Gym session/5/3/1/structure contracts from `FND-20260901-025` and `FND-20260901-026`; the Share-to-Task contract from `FND-20260831-014`; and the verified Track, Goal, typed Settings, Habit, Task, recovery, reminder, and calendar contracts.
- Schema 41 and portable-backup format 18 retain the schema-40 Goal/Gym/Track guarantees and add request-owned Habit timer sessions with portable review/private exact recovery semantics, without rewriting completed workouts or previously recorded facts. See `DEC-20260902-002`, `IMP-20260902-002`, and `VER-20260902-002`.
- The complete expanded Android inventory and focused Goal outcome-aware journeys are recorded in `VER-20260831-013`. API/platform, viewport, accessibility, migration, and release matrices remain mandatory during adversarial QA.
- The Gym tranche now includes 320dp/200% text, fold-responsive semantics, notification races, and upgrade/restore history coverage. Whole-app 200%/320dp, landscape, RTL, TalkBack, fold/tabletop, and API 26/34/37 matrices remain required.
- Behaviorally validate the personal memory skill in future real tasks and refine the schema if agents create duplicates, stale statuses, or excessive prose.

## Canonical ledgers

- [USER_FEEDBACK.md](USER_FEEDBACK.md): requests, use cases, and acceptance criteria.
- [FINDINGS.md](FINDINGS.md): confirmed problems, systemic risks, evidence, and status.
- [DECISIONS.md](DECISIONS.md): product/domain/architecture choices and rejected alternatives.
- [IMPLEMENTATION_LOG.md](IMPLEMENTATION_LOG.md): chronological behavior changes and compatibility notes.
- [VERIFICATION.md](VERIFICATION.md): exact scopes, results, exclusions, releases, and residual risks.

## Detailed source archives

- [Maximum-quality whole-product audit](../WHOLE_PRODUCT_MAXIMUM_QUALITY_AUDIT_2026-08-31.md)
- [Gym 5/3/1 product audit](../GYM_531_PRODUCT_AUDIT_2026-08-31.md)
- [Gym product audit](../GYM_PRODUCT_AUDIT_2026-08-30.md)
- [Full-app UI/UX/design/QA audit](../ux-audits/FULL_APP_UI_UX_DESIGN_QA_AUDIT_2026-08-30.md)
- [Testing strategy and coverage inventory](../testing.md)
- [Architecture](../architecture.md)

## Maintenance contract

Use stable IDs in the form `FB|FND|DEC|IMP|VER-YYYYMMDD-NNN`. Never recycle IDs or erase superseded reasoning. Record feedback before implementation, link implementation to regression evidence, distinguish test compilation from execution, and update this snapshot at the end of every substantial task. After each coherent verified chunk, stage only its owned changes, commit it with a focused message, push normally to its configured upstream, and verify reachability before moving to unrelated work.
