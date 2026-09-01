# Durable product and engineering decisions

### DEC-20260831-001 — Structured strength programs accept arbitrary compatible lifts

- Context: 5/3/1 defaults traditionally emphasize four lifts, but Whip users want other schedules and lifts.
- Decision: Treat the standard four as convenient defaults, not a domain constraint. Permit ordered, distinct compatible exercises such as Bench Press, Deadlift, and Zercher Squat.
- Why this is superior for Whip: It preserves accurate percentage programming while supporting real lifter customization.
- Reversal conditions: Only safety or calculation evidence specific to an exercise capability should restrict selection; names alone must not.
- Status: Accepted and released.

### DEC-20260831-002 — 1RM, e1RM, Training Max, percentage, and working load are separate facts

- Context: Conflation made controls misleading and progression unsafe.
- Decision: Persist provenance and user-adjustable TM percentage; allow direct TM or derivation from actual/e1RM; expose TM outside the 5/3/1 wizard when percentage prescriptions require it.
- Consequences: Historical prescriptions remain snapshots and current edits do not retroactively recompute completed workouts.
- Status: Accepted and released.

### DEC-20260831-003 — Joker is optional and additive

- Context: A Joker toggle should not regenerate or compete with Supplemental work.
- Decision: Model Joker as independently recordable Optional work inserted after Main and before Supplemental, with a dedicated narrow mutation path.
- Consequences: Enabling/disabling Joker preserves exact Main and Supplemental authored data.
- Status: Accepted and released.

### DEC-20260831-004 — Workout instances may diverge from templates without rewriting them

- Context: Lifters need substitutions and one-off exercises during actual training.
- Decision: Store workout-only additions with the session/history as ad-hoc Optional work; do not mutate the routine or future workouts unless the user explicitly edits the routine.
- Status: Accepted and released.

### DEC-20260831-005 — Cycle progression is advisory, explainable, conservative, and per lift

- Context: AMRAP/Joker/test performance can inform the next cycle but does not justify opaque automation.
- Decision: Offer standard, higher, lower, hold, ignore, or custom user-confirmed choices from bounded evidence. Stronger-than-standard recommendations require corroborated independent evidence; skipped optional work is neutral.
- Consequences: Recommendation evidence and decisions are auditable; historical workouts stay immutable.
- Status: Accepted and released; longitudinal calibration remains open to user evidence.

### DEC-20260831-006 — Repository-backed memory is mandatory for substantial Whip work

- Context: Chat compaction and isolated audit reports cannot reliably carry a long-running product program.
- Position A: Add a memory paragraph only to the large goal prompt.
- Position B: Create a reusable skill plus canonical repository ledgers and a goal checkpoint.
- Evidence and constraints: Skills can enforce procedure across tasks; repository files survive chats and are reviewable; neither should override current code evidence.
- Failure modes: Prompt-only memory disappears outside that goal. Unstructured documentation becomes stale or contradictory. Memory treated as truth can preserve outdated claims.
- Decision: Use the personal `maintain-whip-memory` skill, workspace fallback instructions, stable linked ledgers, and mandatory start/during/end writeback in the maximum-quality goal.
- Why this is superior for Whip: It is durable, searchable, auditable, reusable across task sizes, and explicit about evidence quality.
- Status: Accepted and implemented; future-task validation pending.

### DEC-20260831-007 — Version work as coherent pushed outcomes

- Context: Long-running local changes are difficult to audit, bisect, compare, and safely revert.
- Position A: Commit everything once at the end of a large goal.
- Position B: Commit and push each coherent, verified, independently revertible outcome before unrelated work begins.
- Evidence and constraints: Smaller purposeful commits make provenance and rollback clearer, but microcommits and unsafe staging in a dirty worktree reduce signal and can capture user changes.
- Failure modes: End-only commits combine unrelated regressions. File-by-file microcommits may not build. Automatic broad staging can leak secrets, caches, artifacts, or unrelated work. Force-pushing can destroy shared history.
- Decision: Use behavior-level chunks, explicit staging, focused commits, normal upstream pushes, reachability verification, and immediate blocker reporting. Never force-push or rewrite shared history.
- Why this is superior for Whip: It balances traceability and safe reverts with buildable, comprehensible product changes.
- Status: Accepted and implemented in the working protocol.
