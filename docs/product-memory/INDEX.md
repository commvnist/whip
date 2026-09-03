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
- Declared test-source baseline: **1,467 product tests: 552 JVM and 915 Android**. The complete current baseline was freshly executed for `VER-20260902-012`; the most recent separate continuous E2E coverage campaign remains `VER-20260901-022`.
- The latest Gym release includes arbitrary-lift 5/3/1 creation, explicit/derived Training Max controls inside and outside 5/3/1, performance-informed cycle review, workout-only exercise addition, contextual routine return, adaptive routine editing, timer-boundary correction, and additive Joker behavior.
- The verified post-release Gym candidate additionally has exact quick-set/finish and structure boundaries, immutable required Main-work outcomes, retained retired/tombstone history, transactionally coherent active projections, an explicit responsive Arrange editor with value-preserving Undo, recoverable History Copy identities, request-owned lifecycle results, and durable PR/Link/rest-timer reconciliation. See `VER-20260901-018` and `VER-20260901-020`; it is not yet a signed physical-device release.
- The latest verified candidate also makes every typed Settings value an explicit, bounded, request-owned durable edit with strict parsing, commit-failure rollback/retry, lifecycle/conflict protection, and IME/large-text reachability. See `FND-20260831-013`, `DEC-20260901-024`, `IMP-20260901-017`, and `VER-20260901-019`. It is not yet a signed physical-device release.
- Health Connect, Custom Units, backup/restore, and Reset Whip are now independently verified against atomic mirror reconciliation, lifecycle-owned authorship, domain-specific unit semantics, private-versus-portable recovery state, proof-based legacy repair, and an application-wide exclusive maintenance gate. See `FND-20260831-020`, `FND-20260901-027`, `FND-20260901-028`, `IMP-20260901-019`, and `VER-20260901-021`. This remains an emulator-verified candidate, not yet a signed physical-device release.
- Android Share-to-Task and widget Task creation are now bounded before saved state, delivered through an exact saveable FIFO, and prevented from silently replacing an open draft. Unicode-safe shortening, request-owned date/Area defaults, deliberate replacement/defer behavior, and a durable counted queue-overflow acknowledgment are independently accepted and emulator-verified in `IMP-20260901-020` / `VER-20260901-022`. This is not yet a signed physical-device release.
- Habit timers now have a durable request-owned session ledger, canonical Duration conversion, monotonic live display, explicit reboot/restore review, portable/private backup semantics, stable widget actions, and accessible recovery UI. See `FND-20260902-001`, `DEC-20260902-002`, `IMP-20260902-002`, and `VER-20260902-002`. This is an emulator- and release-build-verified candidate, not yet a physical-device release.
- Basic Habit creation now keeps required cadence inline while progressively disclosing reminder overrides, ending rules, and week boundaries. Consequential state remains summarized; configured data, Power Mode, and hidden validation errors reveal the controls automatically. See `FND-20260831-012`, `DEC-20260831-013`, `IMP-20260902-003`, and `VER-20260902-003`.
- Habit Today/Home now separate action-needed work from completed/skipped work; paused and off-schedule states are truthful and protected from accidental quick logging; explicit exceptions and active timers remain reachable; visible timer times use Whip's configured zone. See `FND-20260902-002`, `DEC-20260902-003`, `IMP-20260902-004`, and `VER-20260902-004`.
- Habit History now follows effective dates, includes started pauses as editable neutral events, keeps future pauses in Options, and explains the derived-history impact of pause edits/deletion. Pause-only Insights no longer claims inactivity or a 0% failure. See `FND-20260902-003`, `DEC-20260902-004`, `IMP-20260902-005`, and `VER-20260902-005`.
- Primary navigation now remains visibly named at 150–320% text through measured one/two-row phone geometry and a label-sized scrollable rail. A clear returning Home offers bounded concrete routes back to saved context. See `FND-20260831-011`, `FND-20260831-017`, `FND-20260902-004`, `DEC-20260902-005`, `IMP-20260902-006`, and `VER-20260902-006`.
- Track/Entry discovery now has one explicit workspace owner spanning active and archived data; scope copy remains synchronized and archived Track results return to Archived. Purposeful Activity and per-Track filters remain local. See `FND-20260831-018`, `FND-20260902-005`, `DEC-20260902-006`, `IMP-20260902-007`, and `VER-20260902-007`.
- The whole-product platform/accessibility candidate now has a verified API 26/34/37 matrix, real short-window/software-keyboard remediation, adaptive compact-through-desktop semantic coverage, and actual TalkBack keyboard navigation. See `FND-20260902-006`, `DEC-20260902-007`, `IMP-20260902-008`, and `VER-20260902-008`.
- Area management now keeps create/rename/color/reorder/move/merge/archive/restore/delete owned by the initiating surface through an exact result, preserves drafts and choices on failure, treats post-commit cleanup as warnings, and represents archived identity, search, restoration, color, and conflicts truthfully. See `FND-20260902-007`, `DEC-20260902-008`, `IMP-20260902-009`, and `VER-20260902-009`.
- Tag management now separates global Rename from explicit Merge, updates Task/Habit/Goal/Track references transactionally, keeps archive state stable until explicit restoration, reports cross-domain usage, rejects the CSV separator, and retains exact request/draft ownership in a searchable responsive manager. See `FND-20260902-008`, `DEC-20260902-009`, `IMP-20260902-010`, and `VER-20260902-010`.
- Completed-Workout deletion now reviews one exact revision-tokened impact, blocks active sessions, preserves 5/3/1 Training Max decisions and retired Link/automation audit facts, reconciles personal-record and timer projections after commit, and retains one lifecycle-owned result through failure or process recovery. See `FND-20260902-009`, `DEC-20260902-010`, `IMP-20260902-011`, and `VER-20260902-011`.
- Machine deletion now uses the same exact request lifecycle: stable target/UUID validation, revision-checked impact, one-owner outcome, process recovery, explicit retry/uncertainty, responsive 200%-text review, routine repair disclosure, and preserved completed-workout equipment snapshots. See `FND-20260902-010`, `DEC-20260902-011`, `IMP-20260902-012`, and `VER-20260902-012`.
- Detailed Gym evidence lives in [the 5/3/1 product audit](../GYM_531_PRODUCT_AUDIT_2026-08-31.md) and [testing inventory](../testing.md).

## Active direction

- `FB-20260831-012`: the maximum-quality whole-product backlog remains the product direction. Its initial multi-agent inventory and synthesis are preserved as historical evidence in `VER-20260831-005` and `../WHOLE_PRODUCT_MAXIMUM_QUALITY_AUDIT_2026-08-31.md`. Per `FB-20260902-001` / `DEC-20260902-001`, ongoing work now uses a conventional single-developer loop with no subagents, simulated panels, recursive audits, formal debates, or repeated approval rounds unless the user explicitly requests them.
- `FND-20260831-007` P0 restore recovery, `FND-20260831-008` reminder-delivery integrity, `FND-20260831-009` shared live calendar/time-zone consistency, `FND-20260831-010` Task/Habit/Goal authored-save integrity, `FND-20260831-011` named enlarged-text navigation, `FND-20260831-012` basic Habit schedule disclosure, `FND-20260831-013` typed Settings integrity, `FND-20260831-014` bounded external Task capture, `FND-20260831-015` Goal backup history, `FND-20260831-016` Goal historical IA, `FND-20260831-017` returning-Home recovery, `FND-20260831-018` Track search ownership, `FND-20260831-020` batch-atomic Health reconciliation, `FND-20260901-022` stale Track schema integrity, `FND-20260901-023` direct Track Entry integrity, `FND-20260901-024` CSV batch identity/recovery, `FND-20260901-025` exact Gym session outcomes, `FND-20260901-026` lightweight Gym structure exactness, `FND-20260901-028` restore/reset integrity, `FND-20260902-001` Habit timer integrity, `FND-20260902-002` Habit availability/Today resolution, `FND-20260902-003` effective-date Habit History/pause impact, `FND-20260902-004` adaptive navigation/Home context recovery, `FND-20260902-005` Track/Entry discovery integrity, `FND-20260902-006` whole-product platform/accessibility consistency, `FND-20260902-007` Area-management outcome integrity, `FND-20260902-008` Tag-management integrity, `FND-20260902-009` completed-Workout deletion integrity, and the Habit, Task, Goal, Track, Gym, Custom Unit, Health, Area, and Tag subsets of `FND-20260831-019` / `FND-20260901-027` are resolved and verified. Remaining work proceeds through concrete secondary families and final physical-device release readiness.
- `FB-20260831-013`: keep durable project memory outside chat. The infrastructure and skill package are structurally validated; future task use will validate whether the protocol needs adjustment.
- `FB-20260831-014`: commit and push each coherent verified chunk for easier tracking and reverts. The workflow rule is implemented and structurally validated in the skill, workspace instructions, memory schema, and whole-product goal.
- Treat subjective fixes in release 0.3.34 as awaiting continued real-user validation even where automated and device checks pass.

## Highest-priority unresolved verification

- Continue the remaining risk-proportionate secondary families and final release-readiness pass. Preserve the API 26/34/37 and TalkBack contracts from `VER-20260902-008`; the exact Health/Custom Unit/restore/reset contracts from `FND-20260831-020`, `FND-20260901-027`, and `FND-20260901-028`; the Gym session/5/3/1/structure contracts from `FND-20260901-025` and `FND-20260901-026`; the Share-to-Task contract from `FND-20260831-014`; the Tag contracts from `FND-20260902-008`; and the verified navigation, Home, Track/search, Goal, typed Settings, Habit, Task, Area, recovery, reminder, and calendar contracts.
- Schema 41 and portable-backup format 18 retain the schema-40 Goal/Gym/Track guarantees and add request-owned Habit timer sessions with portable review/private exact recovery semantics, without rewriting completed workouts or previously recorded facts. See `DEC-20260902-002`, `IMP-20260902-002`, and `VER-20260902-002`.
- The complete expanded Android inventory and focused Goal outcome-aware journeys are recorded in `VER-20260831-013`. API/platform, viewport, accessibility, migration, and release matrices remain mandatory during adversarial QA.
- The Gym tranche includes 320dp/200% text, fold-responsive semantics, notification races, and upgrade/restore history coverage. The whole-app 200%/320dp, RTL, TalkBack, fold/tabletop, software-keyboard, and API 26/34/37 baseline is recorded in `VER-20260902-008`; future feature changes must retain it.
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
