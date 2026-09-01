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
