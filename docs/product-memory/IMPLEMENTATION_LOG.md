# Implementation history

### IMP-20260831-001 — Gym and 5/3/1 first-class remediation released

- Behavior changed: Added arbitrary-lift 5/3/1 creation; distinct actual/e1RM/TM semantics and adjustable derivation; ordinary-routine TM controls; typed Main/Supplemental/Assistance/Optional work; additive Jokers; performance-informed cycle review; workout-only exercises; contextual picker return; adaptive routine panes; honest no-history state; accessible timers; and timer-boundary correction.
- Important files: `GymEntities.kt`, `RoutineEntities.kt`, `GymRepository.kt`, `RoutineRepository.kt`, `WhipDatabase.kt`, `FiveThreeOneProgression.kt`, `FiveThreeOneBuilder.kt`, `FiveThreeOneCycleReview.kt`, `FiveThreeOneProgramming.kt`, `RoutineBuilder.kt`, `GymScreens.kt`, `WhipApp.kt`, and related JVM/Android tests.
- Persistence/history impact: Explicit migrations through schema 37 and backup format updates preserve historical workout prescriptions and completed work.
- Compatibility: Existing data installed in place; current and historical records are not silently recomputed from edited templates.
- Commit/push: `5fc98dd` on `origin/main`.
- Related: `FB-20260831-001` through `FB-20260831-010`; detailed audit `../GYM_531_PRODUCT_AUDIT_2026-08-31.md`.
- Verification: `VER-20260831-001`, `VER-20260831-002`.
- Status: Released in 0.3.34; continued field validation applies.

### IMP-20260831-002 — Durable product-memory infrastructure

- Behavior changed: Added a personal `maintain-whip-memory` skill, repository fallback instructions, canonical ledgers, stable IDs, evidence-state rules, and a reusable maximum-quality goal with mandatory memory checkpoints.
- Important files: `/mnt/c/Users/commv/.codex/skills/maintain-whip-memory/`, `AGENTS.md`, and `docs/product-memory/`.
- Compatibility: Documentation/process only; no application data or runtime behavior changed.
- Related: `FB-20260831-013`, `FND-20260831-006`, `DEC-20260831-006`.
- Verification: `VER-20260831-003`.
- Status: Implemented and structurally validated; future-task behavioral validation pending.

### IMP-20260831-003 — Coherent commit-and-push discipline

- Behavior changed: The personal memory skill, workspace fallback, memory schema, and maximum-quality goal now require every completed coherent chunk to be narrowly staged, committed, normally pushed, and verified before unrelated work begins.
- Important files: `/mnt/c/Users/commv/.codex/skills/maintain-whip-memory/SKILL.md`, its memory-schema reference, `AGENTS.md`, and `docs/product-memory/`.
- Compatibility: Workflow-only. It explicitly preserves unrelated dirty-worktree changes and prohibits force-push/history rewriting.
- Related: `FB-20260831-014`, `DEC-20260831-007`.
- Verification: `VER-20260831-004`.
- Status: Implemented and structurally verified; ongoing behavioral validation applies.

### IMP-20260831-004 — Fail-closed startup restore recovery

- Behavior changed: Whip now resolves pending interrupted restore before normal product work and routes live replace restore through a counted application-wide admission/drain barrier. Workers, receivers, schedulers, widgets, Health/background jobs, ViewModel operations, editor/import SavedState, notification/widget actions, and WorkManager startup fail closed or carry the current non-restored data generation. Widget display preferences survive restore while identity-bearing references and snapshots are reconciled; late stale actions cannot alias restored rows with reused numeric IDs. Recovery or runtime initialization failure produces a full-screen accessible blocking state with serialized Retry.
- Important files: `WhipApplication.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `data/RestoreRecoveryManager.kt`, `startup/StartupRecoveryGate.kt`, `startup/UserDataGeneration.kt`, `ui/StartupRecoveryScreen.kt`, affected ViewModels, reminder/timer/portable schedulers and receivers, widget providers/factories/preferences/snapshot cache/configuration Activity, recovery strings, and focused JVM/Android regression tests.
- Persistence/history impact: No schema or backup-format change. A failed rollback, invalid snapshot, or failed recovery rebuild keeps the existing private recovery marker. The marker is removed only after both restore and background-state rebuilding succeed, preserving the prior atomicity boundary and all existing user records.
- Compatibility: Generation zero accepts legacy unversioned SavedState/widget snapshots only before the first replace attempt. The generation token is excluded from backups, increments only after a durable recovery marker exists, and invalidates stale identity references without changing historical workout/task/habit/goal records. Ordinary startup still reaches the existing product flow; other AndroidX Startup initializers remain while WorkManager's initializer is removed and application configuration is provided on demand.
- Commit/push: Focused recovery commit containing this entry on `origin/main`.
- Related: `FND-20260831-007`, `DEC-20260831-008`.
- Verification: `VER-20260831-006`.
- Status: Implemented and verified; physical-device release is deferred until the integrated goal release.
