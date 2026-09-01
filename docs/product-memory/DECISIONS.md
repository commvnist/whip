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

### DEC-20260831-008 — Unresolved restore recovery fails closed

- Context: Normal startup currently continues after recovery failure, allowing new writes into unresolved state.
- Position A: Gate only the visible Activity, cancel its scopes during restore, and rely on the persistent marker/generation checks for later Retry.
- Position B: Use one application-wide counted admission/drain boundary, then generation-scope state and actions that can survive the database replacement.
- Evidence and constraints: Activity-scoped ViewModels, workers, receivers, widgets, schedulers, Health sync, configuration Activities, and process-restorable drafts can outlive ordinary composition. Cancelling every scope risks partial transactions and lost drafts; one global serial mutex would unnecessarily block safe concurrent reads.
- Failure modes: Activity-only gating permits background writes and same-numeric-ID aliasing. Broad cancellation can strand partial multi-step work. An undifferentiated global mutex increases latency and can deadlock the Settings restore initiator. A counted barrier must still version SavedState, widget references, caches, and external action intents that survive replacement.
- Synthesis/decision: Fail closed with an application-level reader admission/drain barrier, a serialized exclusive restore attempt, privileged full background rebuild, preserved marker, non-restored data generation, accessible Retry, and generation-aware persistent/transient surfaces. Existing admitted operations finish; late work is denied; normal access reopens only after authoritative rebuilding succeeds.
- Why this is superior for Whip: It preserves the last trustworthy atomicity boundary and completed admitted work without accepting mixed-generation mutations, stale action aliasing, or a speculative whole-app transaction rewrite.
- Status: Accepted, implemented, and verified.

### DEC-20260831-009 — Reminder workers re-resolve live delivery eligibility

- Context: WorkManager inputs can outlive edits, completions, pauses, skips, moves, and reminder configuration.
- Position A: add persisted schedule revisions and cancellation calls to every mutation, or generalize reminders behind a reusable scheduling DSL.
- Position B: treat every queued delivery as an untrusted versioned claim, resolve exact live domain eligibility at the last responsible moment, and linearize production mutation versus resolve/post without changing user history.
- Evidence and constraints: reminder truth includes Task occurrences, Habit pauses/skips/checklists/source metrics, Goal type/status/deadline, quiet hours, time zone, and custom-unit history—not one entity row. Room changes coroutine identity inside transactions; WorkManager and NotificationManager cannot share Room atomicity; queued work and action intents survive process death and edits.
- Failure modes: a row revision misses settings/source/history changes and requires a schema/backup migration; cancellation-only still loses races; a generic DSL hides domain semantics. Claims alone fail if fingerprints include mutable performance history, if notification actions are weaker, if settings writers lose updates, or if mutation commits can occur between resolve and post. Reentrant coroutine-context locks deadlock across Room; mixed entity/state lock order deadlocks during rebuild.
- Synthesis/decision: use definition-only deterministic fingerprints plus live eligibility, exact action claims, scheduled-versus-snoozed kinds, one-time legacy upgrade, awaited queue operations, bounded source invalidation, a strictly non-reentrant mutation/delivery boundary, raw delegates under one explicit outer owner, entity→state lock order, and a durable deletion-cleanup journal. Missing/malformed/stale work succeeds silently without posting and reconciles authoritative future work.
- Why this is superior for Whip: It covers the real cross-domain sources of reminder truth, preserves all existing history and persistence formats, remains understandable per domain, and makes race outcomes deterministic without a speculative scheduling language or fragile scattered flags.
- Status: Accepted, implemented, adversarially challenged, and verified.

### DEC-20260831-010 — Live time behavior uses one explicit Whip zone and date flow

- Context: System-zone shortcuts and repository-triggered date snapshots can make screens and reminders disagree.
- Position A: Add one application-scoped calendar context over `WhipClock`, including active zone, physical date, cutoff-adjusted logical date, cutoff, and follow-device policy; gate cross-domain rendering until projections share it.
- Position B: Patch Track, Search, Goal, and Gym separately with local tickers and zone arguments.
- Position C: Replace all wall clocks, deadlines, countdowns, dates, and instants with a universal time framework.
- Evidence and constraints: Independent minute loops can advance visible domains in different frames; `LocalDate` alone suppresses same-date zone changes; Track previously sampled only after repository emissions; Search used the device zone; Gym mixed physical and cutoff-adjusted Today. Historical dates and exact instants are provenance, while focus/rest timers and reminder deadlines are separate absolute-time concerns.
- Failure modes: Local patches recur and retain mixed Home state. A universal clock conflates calendar days, historical instants, and elapsed countdowns and destabilizes unrelated systems. A centralized date without zone/follow policy still misses same-date travel changes.
- Synthesis/decision: Route live calendar behavior through one application-scoped `WhipCalendarContext`, invalidate it on settings and Android time/date/zone changes and aligned minute boundaries, and retain the previous rendered snapshot until all date-derived domain states match. Keep specialized absolute timers separate. New records use explicit Whip provenance; explicit historical starts derive from their supplied instant; persisted history is never re-dated.
- Why this is superior for Whip: Every visible domain shares one falsifiable meaning of Today without imposing cutoff semantics on exports, Health windows, timers, reminders, or historical records.
- Status: Accepted, implemented in `IMP-20260831-006` / `IMP-20260831-007`, and fully verified in `VER-20260831-009`.

### DEC-20260831-011 — Save-dependent navigation occurs only after confirmed persistence

- Context: Area scope changes and success notices currently precede asynchronous persistence in several productivity editors.
- Position A: Keep local callback-driven dismissal and move Area navigation into each callback.
- Position B: Treat the existing global `OperationStatus` as the authoritative save outcome for every open editor.
- Position C: Give each authored editor one typed, request-scoped state and a post-commit receipt; keep global status as presentation feedback only.
- Evidence and constraints: Task, Habit, and Goal writes are asynchronous; Area-filtered entities can disappear from scoped projections during an edit; activity recreation can retain a ViewModel while process restoration cannot; repository commit may succeed before reminder/tag refresh; rapid input can race; historical records must not be recomputed; and a user must never be encouraged to retry an entity that already committed.
- Failure modes: Position A duplicates lifecycle and exact-once logic and cannot distinguish commit from follow-up. Position B lets unrelated success/failure dismiss the wrong editor and can render a Snackbar behind a modal. Position C can wedge if terminal outcomes are not explicitly consumed, can become a generic framework if overextended, and must preserve cancellation/fatal-error semantics.
- Synthesis/decision: Use Position C for Task/Habit/Goal authored definitions. Atomically admit only an Idle request, settle only the matching UUID, reclaim unowned terminal results, never adopt another live Running request, block editor input during the write, preserve failure inline, and move/dismiss only after a successful authoritative receipt. Define repository commit as the point of no return: ordinary post-commit work adds warnings; pre-commit failure remains retryable; fatal errors and structured cancellation retain their meaning.
- Why this is superior for Whip: It makes persistence, lifecycle ownership, navigation, and user messaging one falsifiable sequence without coupling domain rules to Compose or inventing a cross-product transaction DSL. It also leaves quick reversible mutations free to use lighter behavior after individual audit.
- Status: Accepted and implemented for authored Task/Habit/Goal definition editors in `IMP-20260831-008`; secondary mutation families remain `FND-20260831-019`.

### DEC-20260831-012 — Large text preserves visible destination names

- Context: At 150% text, six direct phone destinations become icon-only.
- Decision: Prefer a measured named row, then a stable two-row named layout, then a labeled drawer only when neither fits. Do not use an arbitrary normal-size More bucket.
- Why this is superior for Whip: It preserves spatial memory and one-tap access while making increased text improve comprehension.
- Status: Accepted; implementation pending.

### DEC-20260831-013 — Habit reminders stay visible while advanced schedule controls are disclosed

- Context: Hiding everything weakens reminder awareness; showing every weekday/end/week-boundary option overloads simple creation.
- Decision: Keep a concise reminder summary in the basic path and disclose advanced controls, auto-expanding for existing data, power mode, or validation errors.
- Status: Accepted; implementation pending.

### DEC-20260831-014 — Preserve meaningful legacy Goal history, not every retired table

- Context: Public completion snapshots are user history; some other retained tables merely duplicate canonical state.
- Decision: Preserve or migrate completion snapshots through backup/restore. Omit redundant internals only after proving canonical equivalence and explicitly retiring their lifecycle.
- Status: Accepted; implementation pending.

### DEC-20260831-015 — Habit authored history is request-owned; lightweight reversible actions remain lightweight

- Context: Habit value, history, pause, and skip dialogs dismissed after dispatch, while Home and the Habit workspace shared one ViewModel outcome channel. A generic optimistic path could lose drafts or lie after deletion; applying a modal coordinator to every quick increment would make routine check-ins irritating.
- Position A: Keep optimistic dispatch-and-dismiss for all Habit mutations and rely on global status.
- Position B: Route every Habit action, including single-tap increments and checklist toggles, through one blocking authored-mutation coordinator.
- Evidence and constraints: Log/pause edits carry user-authored drafts and historical meaning; deletes can remove the live row before result delivery; reminders are post-commit derived work; Home/workspace surfaces may exchange ownership; numeric IDs can alias after replace restore; current-total Set is absolute and custom-unit-aware; rapid skip undo must be exactly once. Quick increments and check-offs are intentionally low-cost, immediately reversible interactions.
- Failure modes: Position A loses retry context, permits duplicate destructive taps, and can mutate a wrong restored child without parent validation. Position B adds modal latency and excess coordination to frequent one-handed actions, and one undifferentiated surface can steal another surface's terminal result. Unbounded relative floating tolerance can also silently erase real large-value changes.
- Synthesis/decision: Use typed request ownership for draft-bearing or destructive Habit history, pause, absolute-total, and skip-undo flows. Bind child mutations to the expected Habit inside the transaction; retain saveable child snapshots; namespace each UI surface; let another namespace reclaim only an abandoned terminal after an owner grace period; reset state at the user-data generation boundary; treat the Room mutation as commit and reminders as warning-capable follow-up. Keep ordinary quick increments/check-offs on the existing lighter path. Compare Set totals with a small ULP-bounded noise tolerance, not a magnitude-relative epsilon.
- Why this is superior for Whip: It protects authored data, history, lifecycle recovery, and cross-surface correctness exactly where failure is costly while preserving the speed of everyday Habit check-ins.
- Status: Accepted, implemented, independently challenged, and fully verified for the Habit secondary-mutation family; other `FND-20260831-019` families remain open.
