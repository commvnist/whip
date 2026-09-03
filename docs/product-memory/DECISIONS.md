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
- Status: Implemented in `IMP-20260902-003` and verified in `VER-20260902-003`.

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

### DEC-20260831-016 — Task edits preserve exact occurrence identity and authored Open overrides

- Context: Recurring Task edits span a definition, generated virtual occurrences, persisted exceptions, Subtask state/history, Goal Links, automations, reminders, and lifecycle-restored editor drafts. Timestamp-only conflict checks and position-based child copying could silently overwrite same-millisecond edits, attach integrations to the wrong Subtask, or preserve data that no consumer could reach.
- Position A: Treat the current recurrence rule and latest Task row as authoritative; copy future children by position; dismiss secondary dialogs after dispatch; release a notification action claim after any exception.
- Position B: Capture a compact saveable semantic boundary; validate definition/occurrence/Subtask state transactionally; map retained children by stable ID; migrate compatible Open intent across a series split; treat explicit Open rows as authored overrides; report authoritative commit separately from fallible follow-up.
- Evidence and constraints: Closed occurrences and Subtask snapshots are historical facts. Subtask toggles intentionally create state without occurrence rows. A new cadence or completion anchor can stop generating that date. Inbound automation duplication can double-fire; copied Goal Links must not start before either the split or their configured window. Reminder and deletion follow-up may fail after the Room mutation is already durable.
- Failure modes: Position A loses drafts on lifecycle/failure, permits stale overwrites, can orphan future progress, weakens Goal windows, duplicates series boundaries, and offers unsafe retries after committed deletion or notification action. An indiscriminate full-snapshot/generic-DSL approach would bloat saved state and make frequent interactions needlessly modal.
- Synthesis/decision: Use a compact semantic `TaskEditBoundary` and exact repository preconditions. Split only an open recurring occurrence; preserve the historical definition before the boundary; retain finite remaining counts; map retained Subtasks/integrations by stable ID; retarget inbound automations and copy outbound rules; migrate compatible Open state, materializing state-only occurrence identities; preserve `max(split, configured Link start)`; union generated dates with explicit Open rows in UI and reminder consumers. Use typed request receipts for draft-bearing/destructive flows, exact undo predicates, and phase-aware committed-warning/cancellation semantics.
- Why this is superior for Whip: It keeps real history immutable, makes customization and offline/lifecycle recovery trustworthy, preserves one-handed quick interactions where safe, and gives every persisted authored occurrence a consistent visible/remindable consumer path without inventing a generic scheduling DSL.
- Status: Accepted after three challenge rounds, implemented in `IMP-20260831-010`, and fully verified in `VER-20260831-012`; Goal/Track/Gym subsets of `FND-20260831-019` remain open.

### DEC-20260831-017 — Goal lifecycle, history, and authored mutations use risk-proportionate exactness

- Context: Goal edits previously mixed optimistic dialog dismissal, status-based archive, live terminal recomputation, numeric-only snapshot identity, incomplete deletion impact, and UI-owned business rules. The result could lose retry drafts, mislabel abandonment, rewrite the meaning of a completed Goal, duplicate restored history, or offer a destructive retry after an authoritative commit.
- Position A: Keep dispatch-and-dismiss, use one broad entity revision, represent archive as a lifecycle status, recompute terminal cards from live Goal state, and treat all Measurement writes as generic upserts.
- Position B: Give every Goal interaction a blocking full-snapshot coordinator, freeze all history permanently, reject any post-close correction, and introduce a generic programming/mutation DSL.
- Evidence and constraints: Definition edits, progress drafts, reset times, lifecycle changes, deletion, reminders, backup merge, Home/workspace ownership, replace restore, custom units, and Area deletion have different risk and latency. Historical entries may require factual correction, but a terminal outcome must describe what happened when the Goal closed. Archive answers organization, not outcome. Existing backups lack stable closure identity. Quick pin/milestone actions must remain one-handed and responsive. Health Connect separately relies on deterministic first-import upserts.
- Failure modes: Position A loses drafts, accepts stale edits, conflates Archived with Completed/Abandoned, retroactively changes terminal results, duplicates merge history, and can lie after post-commit cleanup failure. Position B makes routine interactions irritating, freezes correctable evidence, over-couples unrelated domains, and creates more state than lifecycle restoration can safely own.
- Synthesis/decision: Use semantic Goal boundaries scoped to each mutation; request-own draft-bearing, lifecycle, archive, reset, and destructive flows; retain exact lightweight pin/milestone actions. Model archive orthogonally. Permit exact correction/deletion of Goal-owned historical measurements after close/archive while freezing closure outcomes. Persist specialized elapsed/milestone closure data and stable UUIDs; preserve reset events as immutable history. Reject future progress and require explicit confirmation for History-only dates outside the Goal window. Treat Room commit as authoritative and reconcile fallible cross-system work as warnings; fatal errors still escape. Keep ordinary Measurement edits update-only, with an explicitly narrow stable-ID insertion exception only for identified Health Connect reconciliation.
- Resolved disagreements:
  1. Optimistic dismissal versus request ownership: request ownership wins for draft-bearing or consequential actions because failure must retain input and exact retry meaning; lightweight exact actions remain direct.
  2. Full Goal snapshot versus semantic boundary: semantic boundaries win because progress should ignore unrelated pin changes while definition, aggregation, date-window, lifecycle, archive, and target changes invalidate it.
  3. Immutable ledger versus correctable evidence: exact Goal-owned entries remain correctable, but terminal snapshots never recompute, preserving both factual correction and historical truth.
  4. Archive status versus orthogonal archive: orthogonal archive wins because organization cannot erase Completed versus Abandoned outcome.
  5. Live terminal projection versus specialized snapshot: frozen value/progress, elapsed duration, and milestone counts win because later edits/reopens must not rewrite closure meaning; legacy absent fields render truthful fallback rather than live claims.
  6. Numeric snapshot IDs versus stable UUIDs: stable UUIDs win because portable merge must be idempotent across database-local IDs.
  7. Silent future/out-of-window progress versus explicit intent: future records are invalid; out-of-window historical evidence requires an explicit History-only confirmation.
  8. Globally relaxed upsert versus typed reconciliation intent: authored edits remain strict; only Health Connect may recreate an absent row whose deterministic ID exactly matches its source.
- Why this is superior for Whip: It protects lifecycle truth, authored drafts, restore/merge identity, reminder correctness, and destructive-action integrity without turning fast gym/productivity interactions into a generic modal framework.
- Status: Accepted after independent Director, data-integrity, and adversarial-QA challenge; implemented in `IMP-20260831-011` and fully verified in `VER-20260831-013`. Track/Gym subsets of `FND-20260831-019` remain open.

### DEC-20260901-018 — Protect active Gym programming before Track schema remediation

- Context: The secondary-mutation audit found two P0 paths: Gym deletion could erase active 5/3/1 main work and corrupt Training Max progression, while a stale Track schema confirmation could erase newly added unreviewed values.
- Position A: Fix Track first because its stale confirmation can destroy historical values.
- Position B: Fix Gym first because an in-gym destructive action can erase the live prescription and change the next cycle's Training Max decision.
- Evidence and constraints: Both need exact transactional boundaries. The Gym path is reachable during the highest-distraction, lowest-attention workflow and crosses active set truth, required-main-work eligibility, Routine provenance, and 5/3/1 progression. Track requires a larger definition/editor contract that should remain a separate reviewable chunk. Visual polish, search, and unrelated Settings work provide no comparable correctness reduction.
- Failure modes: Track-first leaves an active strength-program corruption path shipping longer. Gym-first leaves known Track stale-schema loss pending. Combining them makes regression ownership, rollback, and historical compatibility harder to audit.
- Synthesis/decision: Remediate Gym permanent Exercise/Routine deletion first as one coherent exact-mutation tranche, commit/push it after complete gates, then begin Track definition/schema integrity immediately.
- Why this is superior for Whip: It removes the live programming-corruption path without diluting either domain's invariants, and preserves a clean commit boundary for the next P0.
- Status: Accepted and executed for `IMP-20260901-012`; Track remains `FND-20260901-022`.

### DEC-20260901-019 — Gym deletion uses exact impact, active guards, and durable outcome ownership

- Context: Permanent deletion crosses Room cascades, active workouts, Routine templates, performed history, PRs, Training Max audits, graph presets, machines/categories, Goal Links, Triggers, automation-created Track entries, settings shortcuts, and fallible post-commit reconciliation.
- Position A: Keep the existing immediate confirmation and generic global operation status.
- Position B: Never permit permanent Exercise/Routine deletion; archive only.
- Position C: Freeze a complete dependency impact, block only active programming hazards, validate the exact revision transactionally, and return a request-owned committed receipt with durable unknown-outcome verification.
- Evidence and constraints: Users explicitly need archive and permanent cleanup, but historical and program consequences must be intelligible. Completed workout placements/sets are deleted only when the user reviews that exact count; Routine deletion instead preserves completed/discarded workout snapshots by clearing the mutable source reference. Training Max decisions already contain immutable Routine/Exercise identity snapshots and must remain as audit history. Process death may occur before the UI learns whether Room committed.
- Failure modes: Position A permits stale confirmation, false retries, wrong-surface delivery, and active progression corruption. Position B prevents deliberate cleanup and leaves no exact impact contract. Position C can misclassify an unknown process-death outcome unless ownership survives SavedState and missing-target UI continues to offer verification.
- Synthesis/decision: Use Position C. SHA-256 revisions include the root and every affected dependency; active Exercise placements/alternatives and active Routine-sourced sessions block deletion; exact counts must match inside one transaction; immutable Training Max decisions and explicitly preserved workout snapshots remain; ordinary post-commit PR/Link/settings failures become warnings; fatal errors still escape. The request/recovery token lives in `SavedStateHandle`, candidates/generation live in saveable UI state, and Retry Verification transfers ownership to a fresh read-only verification request.
- Why this is superior for Whip: The user can make an informed permanent-cleanup choice while Whip protects live programming, preserves audit truth, rejects stale impact, and never encourages a second destructive write merely because delivery was interrupted.
- Status: Accepted after repeated UX/engineering/QA challenge and fully verified in `VER-20260901-014`.

### DEC-20260901-020 — Track definition saves use a compact authored boundary plus exact removal review

- Context: The Track editor persists a complete definition draft but authorizes destructive Field and Choice changes with raw database IDs and opening-time Entry counts. A concurrent editor or CSV import can add Fields, Choices, values, or dormant Link/Trigger references after confirmation; the stale save can then delete or retarget rows the user never reviewed. Track saves also report fallible tag follow-up as if the authoritative Track write failed.
- Position A: Freeze the entire Track projection, including all Entries and values, and reject any intervening change before every definition save.
- Position B: Keep the existing definition save and strengthen only each destructive confirmation with a count or per-removal token.
- Evidence and constraints: A harmless new Entry must not block a rename, reorder, or same-dimension default-unit edit. Conversely, a concurrent empty Field/Choice is absent from a stale full-definition draft and would be silently deleted without a whole authored-definition precondition. Same-count value edits and newly added compatibility references defeat count-only review. Link and Trigger execution was deliberately retired in schema 31; its hidden rows remain dormant compatibility/audit data and cannot become an uneditable blocker. Existing pin, archive, list position, denormalized Area labels, search rows, historical generated facts, and unrelated Entry values are outside this editor's authored definition.
- Failure modes: Position A creates nuisance conflicts during ordinary logging/import, produces an unbounded lifecycle payload, and couples definition editing to unrelated history. Position B still permits stale schema overwrite, same-count history mutation, replacement-target drift, and unreviewed legacy-reference changes. Blocking every dormant integration reference would dead-end users because Whip no longer exposes an editor for retired automation rules.
- Synthesis/decision: Use two exact boundaries. A compact semantic revision covers Track identity and editable metadata plus every ordered Field/Choice identity and property, excluding pin/archive/order, timestamps, derived Area name, Entries, and values. A repository-originated removal review separately fingerprints the normalized destructive plan, exact affected value rows, replacement identity, and every affected dormant Link/Trigger row. Validate both inside the same Room transaction before any write; derive removals from the draft rather than accepting confirmed IDs. Any definition or reviewed-impact change retains the draft and requires renewed review. Continue the schema-31 compatibility behavior for explicitly reviewed dormant references, disclose it as legacy integration impact, and preserve generated historical facts. Request-own the save, treat the Room write as authoritative, and downgrade fallible tag/Area verification to committed warnings.
- Why this is superior for Whip: It prevents unreviewed history loss and stale schema overwrite while allowing unrelated one-handed logging and imports to continue. It preserves upgrade compatibility without reviving a retired automation product, keeps SavedState compact, and makes retry behavior truthful after the commit point.
- Status: Accepted after domain, UI, product, and adversarial-QA challenge; implemented and fully verified in `IMP-20260901-013` / `VER-20260901-015`.

### DEC-20260901-021 — Track Entry edits use exact rejection and stable create identity

- Context: Entry is a historical fact, but current create/update/delete flows share global status and identify edits only by numeric ID. A stale full-form draft can erase a concurrent value; rapid or unknown-outcome create can duplicate history; delete/Undo can separate a restored Entry from its fulfilled TriggerOccurrence.
- Position A: Automatically merge Field-level changes from concurrent Entry editors.
- Position B: Keep last-write-wins but warn users that another writer may have changed the Entry.
- Position C: Freeze a compact opening form contract plus the exact Entry/value revision, reject any conflicting update/delete transactionally, retain the draft, and preallocate stable create identity so exact retry is idempotent.
- Evidence and constraints: Values are keyed by stable Field UUID, which makes exact comparison reliable, but a full three-way merge must also resolve Field removal/type changes, required additions, Choice membership, units, provenance, and user intent. Timestamp-only checks fail for same-millisecond writes. Freezing unrelated Track decoration or all other Entries would create nuisance conflicts. Field/Choice labels still convey user-facing meaning and cannot safely be treated as purely cosmetic while a form is open. Existing Room transactions and search atomicity are sound foundations. Automation execution is retired, but preserved TriggerOccurrence/Contribution facts remain audit history.
- Failure modes: Position A can silently combine incompatible intent or reinterpret values after schema changes. Position B knowingly permits lost updates and cannot make retries truthful. Position C can create nuisance conflicts if its form scope is too broad and cannot provide durable Undo across process death without a tombstone; it must distinguish exact achieved retry from mismatch and explain same-process Undo limits honestly.
- Synthesis/decision: Use Position C. The form boundary covers stable Track identity/writable state and user-facing Field/Choice meaning needed to interpret the Entry, while excluding Track description/icon/Area/tags/pin/order, Field list-display decoration, unrelated Entries, timestamps as authority, and rebuildable search. The Entry boundary hashes stable identity, date, immutable provenance, and every typed value identity/content using raw numeric bits. Create receives a process-saveable preallocated Entry UUID and returns an exact transaction receipt; update/delete reject stale boundaries without mutation. Request/session/generation owns delivery. New authorship freezes the complete visible unit contract, while Undo accepts harmless unit rename/archive decoration only when stable ID, dimension, and raw conversion semantics still match. Delete snapshots and atomically restores compatible occurrence fulfillment for same-process Undo; incompatible restore rolls back. Whip exposes one exact Undo at a time: confirming a newer deletion supersedes an untouched prior Undo, but a failed restore remains retryable and cannot be overwritten. CSV batch idempotency remains a separate bounded protocol.
- Why this is superior for Whip: It protects historical truth and user intent with a small domain-specific contract, makes mobile double taps and exact retries safe, preserves editable drafts on conflict, and avoids both a generic merge engine and coupling one Entry to unrelated Track activity.
- Status: Accepted after independent domain, UI/accessibility, adversarial QA, and Director challenge; implemented and fully verified in `IMP-20260901-014` / `VER-20260901-016`.

### DEC-20260901-022 — Track CSV imports use private receipts plus deterministic row identity

- Context: A complete CSV transaction could commit while its UI callback was lost, leaving a restored session that offered the same batch again with fresh Entry UUIDs. The protocol must distinguish an exact completed retry from intentional duplicate content, preserve historical edits/deletions, keep portable backups clean, and remain usable at 5,000 rows without binding persistence to mutable UI projections.
- Position A: Deduplicate by visible row content or payload fingerprint. This is simple but prevents intentional duplicate facts, cannot distinguish edited/deleted imported history, and makes later semantic changes retroactively affect identity.
- Position B: Use deterministic Entry UUIDs only. This prevents duplicate inserts, but a stale retry after all imported rows were deliberately deleted could resurrect history and cannot prove that an entire batch, search index, and receipt committed together.
- Position C: Give each reviewed session a fresh batch UUID; freeze an exact versioned request and deterministic per-row identities; atomically insert Entries, values, search rows, and a private constant-size receipt; verify the full receipt envelope before reading the source URI on recovery.
- Evidence and constraints: Track numeric IDs are database-local, so the envelope also binds stable Track UUID/creation identity. Field/Choice labels and selected unit conversion contracts affect interpretation and must share the opening snapshot. URI, filename, headers, mappings, and values must not be retained in the receipt. Portable backup format 16 already preserves Entry UUIDs and historical facts; operational retry proof should not cross devices. A maximum import permits 5,000 rows and 100,000 cells, so per-cell live-unit queries and per-row form reloads are unacceptable.
- Failure modes: Position A silently drops legitimate duplicates and conflates current display with historical authorship. Position B can resurrect deliberately removed facts and offers no exact achieved-state proof. Position C can become overly conservative or slow if it fingerprints unrelated mutable units, trusts a newer form after parsing, weakly compares restored receipts, or validates custom units per cell.
- Synthesis/decision: Use Position C. Parse only against a repository-originated atomic `TrackEntryFormSnapshot`; revalidate the same Track, Field, Choice, writable, and selected/default unit contracts during preparation and commit; canonicalize with versioned SHA-256 and deterministic UUIDv8 identities; compare the complete stable receipt envelope as Missing/Exact/Collision; reject archived non-default units while retaining an archived Field default. Keep receipts Track-cascaded, digest-only, excluded from portable backup, and invalidated with user-data generation on replace restore. Build FTS from the validated in-memory form, checkpoint cancellation, and validate each distinct used unit once per transaction.
- Why this is superior for Whip: Exact retries are safe without content deduplication, intentional duplicate history remains possible, edits/deletions are never resurrected, process recovery is truthful even when the URI is unreadable, schema/unit races cannot reinterpret facts, and the product-level 5,000-row/100,000-cell contract is measured rather than assumed.
- Status: Accepted only after repeated domain, UX/accessibility, and adversarial-QA rejection/remediation cycles; implemented in `IMP-20260901-015` and fully verified in `VER-20260901-017`.

### DEC-20260901-023 — Gym commits exact facts; derived work reconciles from them

- Context: Active Gym mutations cross performed Set truth, optional work, retired placements, Routine provenance, 5/3/1 progression, personal records, Links, rest notifications, Activity/process lifecycle, and independently invalidated Room streams. The product must reject a genuinely stale author without making normal one-handed execution race its own UI projection.
- Position A: Keep best-effort UI callbacks and global status, then rebuild derived state opportunistically. This minimizes new types but cannot identify the exact editor, Set, session, timer generation, or commit point.
- Position B: Freeze or rewrite the entire workout graph for every action. This is maximally conservative but turns independent Set logging into nuisance conflicts, creates large lifecycle payloads, and risks retroactively recomputing performed history from mutable program definitions.
- Relevant evidence and constraints: A quick Set has stable Set/workout UUIDs and narrow revisions; finish has a specific session UUID/revision and optional Training Max decisions. Required Main-work eligibility, authored classification, program provenance, equipment, and units must survive later edits. Joker Sets are optional additions, not replacements for BBB/FSL/BBS. PRs, Links, and notifications are reconstructible; the committed workout is not. WorkManager enqueue/cancel completion and notification delivery are asynchronous. Room invalidates observed tables independently unless the active graph is reread transactionally.
- Failure modes: Position A permits duplicate/stale saves, wrong-surface dismissal, false destructive retry, stale timer delivery, and progression from missing evidence. Position B blocks harmless parallel work, bloats state, obscures precise conflicts, and couples historical truth to a generic snapshot/DSL. Treating every follow-up failure as commit failure can duplicate the authoritative action; clearing timer state before notification delivery can lose the only retryable proof.
- Synthesis/decision: Use narrow exact boundaries and immutable historical facts. Quick-set submit binds Set UUID/update time plus current workout revision; finish binds session UUID/revision and explicit 5/3/1 decisions. Placement and Set outcomes retain completed facts while removing execution eligibility. `requiredForProgressionSnapshot`, authored classification, work-section, optional-kind, equipment, unit, TM, and program provenance remain frozen. Active UI consumes one transactionally coherent Room graph, and newly added focus is retained until that graph contains the placement. Request-owned receipts cross Activity recreation; unknown process recovery warns rather than guessing. Room commit is authoritative. PR/Link/timer projections reconcile from durable rows. Timer schedule/cancel is awaited, revision/deadline acknowledged exactly, and notification-first delivery is at least once and idempotent.
- Reason the selected design is superior for this application: It protects serious strength-programming semantics and completed history while preserving fast independent Set entry. It makes conflicts small and intelligible, keeps Joker/supplemental/assistance meaning explicit, survives lifecycle and scheduler failure, and provides a reusable fact-plus-projection pattern without inventing a generic workout DSL.
- Status: Accepted after multiple domain, lifter/UX, engineering, accessibility, and adversarial-QA challenge rounds; implemented in `IMP-20260901-016` and fully verified in `VER-20260901-018`.

### DEC-20260901-024 — Typed Settings use explicit modal drafts and durable request receipts

- Context: Typed Settings formerly wrote every parseable keystroke. The first remediation tried inline drafts with focus-loss/IME commit; later proposals used a modal plus timeout or observation of the current preference as success. Each could still lose intent during disposal, deadlock on an unchanged value, accept a late write after a timeout, or mistake process-local `SharedPreferences` publication for durable persistence.
- Position A: Retain inline fields, commit on focus loss/IME, and infer completion from callbacks or the observed current value. This minimizes taps and keeps values visible in-place.
- Position B: Use a focused modal draft with explicit Save/Cancel, strict complete-field parsing, and one request-owned repository receipt that confirms the durable write before dismissal.
- Relevant evidence and constraints: Typed fields include seconds, days, percentages, counts, exact `HH:MM`, region/fixed-offset zones, and coupled quiet-hours state. Android `SharedPreferences.commit()` can return false after exposing a process-local attempted value. Activity recreation, Back/Escape, parent-mode changes, rapid double submit, oversized paste, the IME, 320dp width, 200% text, and concurrent Settings surfaces all affect authorship. Fast boolean/list choices should not inherit modal friction without evidence.
- Failure modes of Position A: Focus disposal can skip the final edit or write a parseable prefix; a global/current-value observer cannot identify the initiating request; a timeout creates a late-outcome ambiguity; an unchanged or normalized value can wait forever; and a failed disk commit can still make the attempted value look current in process memory. Position B adds one tap and can hide external changes unless the modal tracks semantic source identity, retains draft/conflict state, and keeps its actions above the IME.
- Synthesis/decision: Use Position B only for typed Settings. The modal owns a bounded saveable draft, semantic opening/source identity, explicit conflict and discard behavior, and a durable-retry obligation. Save admission is atomic and request-scoped. Confirmed repositories serialize authored writes, use durable commit, restore the prior process-visible state on commit failure, and settle the exact receipt off the main thread. While a request runs, observed values never advance the durable baseline. Parent modes and section navigation cannot silently invalidate the child editor. Explicit Cancel may discard; Back, outside dismissal, and Escape require confirmation whenever uncommitted intent or durability ambiguity remains. Unchanged values avoid writes unless a prior failed request still requires durable retry.
- Reason the selected design is superior for this application: It trades one deliberate tap for truthful, testable configuration authorship across Gym, Health, reminders, backup, and time policy. It supports one-handed and assistive use, distinguishes region zones from follow-device/fixed-offset modes, survives normal lifecycle changes, and reuses Whip's narrow persistence coordinator without spreading a generic form DSL or modalizing harmless toggles.
- Status: Accepted after the inline, timeout, process-observation, commit-failure, and Back-dismiss proposals were challenged and corrected by domain, UX/accessibility, and adversarial-QA specialists; implemented in `IMP-20260901-017` and fully verified in `VER-20260901-019`.

### DEC-20260901-025 — Gym layout uses an exact batch Arrange editor; discrete mutations remain narrow

- Context: Active-workout exercise order, groups, retired placements, tombstones, and Set rows form one historical layout, but normal logging must stay fast and legible. The previous always-available reorder/remove callbacks could race one another, advance revisions for a semantic no-op, restore into a newer position collision, or deliver a failure to the wrong surface.
- Position A: Refine always-visible drag/reorder controls and persist every gesture immediately. This minimizes mode switching and resembles a generic list editor.
- Position B: Put structural layout changes in a dedicated batch Arrange editor, while retaining narrow exact transactions for discrete add, remove, group, machine, Set, discard, and History-copy actions.
- Relevant evidence and constraints: In-gym users operate one-handed with limited attention; drag affordances compete with Set logging; groups require contiguous collision-free order; retired placements and tombstones remain historical evidence; exact layout Undo must not roll back newer Set values or completion; and History Copy can cross route or process reconstruction. The existing stable UUIDs and session revisions support precise boundaries without a generic programming DSL.
- Failure modes of Position A: Normal workout cards stay visually noisy, partial gesture persistence creates excess revisions and stale conflicts, group invariants can be exposed between writes, and a broad Undo can erase unrelated newer work. Position B adds an explicit mode switch and can reject an old batch after concurrent change; it also fails if its fingerprint includes mutable Set performance or if global busy/error state leaks between owners.
- Synthesis/decision: Select Position B for layout. Capture an exact versioned structure boundary, include active and retired placement/group/Set structure, exclude Set values/completion from the canonical fingerprint, and commit the whole collision-free arrangement atomically. Scope its Undo to the same session/data generation and preserve newer Set values. Keep discrete operations as stable-identity, replay-safe exact transactions. Give active-session and History Copy separate lifecycle-persistent coordinators, while combining their busy state only to prevent conflicting interaction.
- Reason the selected design is superior for this application: It keeps the normal training surface fast and glanceable, makes advanced structure editing deliberate and reversible, preserves completed facts and tombstones, and gives each action a small intelligible conflict boundary. It supports serious Gym customization without imposing a generic builder or freezing the full workout graph for every Set edit.
- Status: Accepted after domain, lifter/UX/accessibility, and adversarial-QA specialists repeatedly challenged tombstones, value-preserving Undo, lifecycle ownership, responsive controls, and cross-coordinator races; implemented in `IMP-20260901-018` and fully verified in `VER-20260901-020`.

### DEC-20260901-026 — Whole-app destructive maintenance closes one global data-access gate

- Context: Reset Whip touches portable-backup ownership, Health reconciliation/deletion, reminder claims and notifications, every Room table, background workers, active editors, and runtime projections. The existing Health and reminder locks did not prevent an already-admitted Gym or other repository mutation from committing after the database clear.
- Position A: Keep local subsystem locks and add more reset-specific cancellation calls. This is smaller and lets unrelated work continue during reset.
- Position B: Close one application-wide admission gate, drain every active data lease, quiesce workers/runtime, advance the user-data generation, perform reset under a documented lock order, rebuild runtime, and only then reopen access.
- Relevant evidence and constraints: A production `CoordinatedMeasurementRepository` concurrency test proved the Health → reminder → Room order does not deadlock. A reset-vs-Gym regression proved Position A could report success while a late writer recreated user data. Reset has no rollback marker, so failures must leave access blocked only while runtime cannot safely resume, not silently pretend atomic rollback exists.
- Failure modes of Position A: It requires an ever-growing list of domain locks, misses new repositories, permits late commits, and makes destructive success dependent on cancellation timing. Position B temporarily hides normal runtime and can deadlock if the caller already owns a data lease or if lock order is inconsistent.
- Synthesis/decision: Select Position B. `SettingsViewModel` invokes reset outside a normal data-access lease; `StartupRecoveryGate.runExclusiveMaintenance` closes admission and drains active owners; `WhipApplication.resetAllData` clears portable-folder ownership and acquires Health → reminder → Room, advances generation before deletion, cancels visible notifications, and rebuilds background/runtime state before `Ready`. Late access is rejected rather than queued under stale ownership.
- Reason the selected design is superior for this application: Reset becomes a truthful whole-product operation that remains correct as Whip gains domains. The brief recovery surface is safer and easier to test than scattered cancellation, and generation invalidation prevents stale editors from acting on the new empty dataset.
- Status: Accepted after engineering, UX, and adversarial-QA challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-027 — Portable backups exclude device-local recovery state; private rollback preserves it

- Context: Health deletion/sync journals and last-action receipts are required to recover interrupted work on the current installation, but moving them to another device can trigger irrelevant cleanup or describe provider state that does not exist there. Replace restore also needs a private pre-restore snapshot capable of returning the current installation to its exact operational state.
- Position A: Use one backup payload for both user export and internal rollback. This is simple and guarantees every current field round-trips everywhere.
- Position B: Define one historical/user-data contract with two export profiles: portable export omits device-local operational state, while a private recovery snapshot preserves it for same-install rollback.
- Relevant evidence and constraints: Completed workouts, custom units, user Exercises, performed sets, Training Max history, and other authored facts must remain portable. Health journals/receipts are operational proof scoped to a provider installation. Removing them from internal rollback would make a failed restore lose pending recovery state; exporting them to another device would create false work.
- Failure modes of Position A: Cross-device restore can inherit stale provider cleanup and misleading sync outcome. Position B can accidentally omit authored history if profile selection is broad or ad hoc, and a private recovery export must never become the user-visible portable artifact.
- Synthesis/decision: Select Position B with an explicit `exportRecoveryBackup` entry point used only by `RestoreRecoveryManager`. Portable export excludes only enumerated local Health journal/receipt fields; private rollback includes them. Import remains backward-compatible and completed historical facts are not recomputed.
- Reason the selected design is superior for this application: It separates user-owned history from installation-owned recovery proof without introducing two schemas, preserves exact rollback, and makes cross-device restore unsurprising and safe.
- Status: Accepted after backup/recovery and Health challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-028 — Restore compatibility is proven against each domain’s real unit contract

- Context: A structurally valid backup can contain values that are impossible for the owning feature: unknown/custom units where Gym accepts only built-ins, mismatched entered/canonical nullability, incorrect dimensions, non-finite canonical conversion, duplicated stable identities, or a machine load inconsistent with its interpretation. A generic “unit exists and dimension matches” check is insufficient.
- Position A: Accept any registered compatible unit and let each repository normalize after restore. This is flexible and minimizes preflight code.
- Position B: Before replacement, validate every unit-bearing fact against the actual historical contract of its domain, including canonical parity, raw conversion, stable identity, and feature-specific restrictions; grandfather legacy display labels that do not alter conversion semantics.
- Relevant evidence and constraints: Gym prescriptions/workouts/TM decisions/PRs use built-in load and distance semantics; Tracks, Goals, Habits, and Measurements can intentionally use custom units. Historical rows must describe what happened, not be reinterpreted by current defaults. Existing users may have older long custom-unit names/symbols even though new UI authorship is now bounded.
- Failure modes of Position A: Restore can commit impossible facts, overflow canonical values, or silently reinterpret history. Position B can reject legitimate old data if it applies current UI length limits retroactively or hardcodes a generic unit whitelist detached from the owning model.
- Synthesis/decision: Select Position B. Preflight walks Measurements, Habit logs, Goals, Tracks, exercises, machines, routines, workout history, TM snapshots/decisions, PRs, and automation constants. It enforces value/unit and entered/canonical null parity, finite expected canonical values, actual Gym built-in/canonical contracts, machine-aware load semantics, and exact stable identities. Existing custom-unit labels are grandfathered; new names/symbols remain bounded at the authoring UI.
- Reason the selected design is superior for this application: Restore fails before destructive replacement when historical meaning cannot be proven, while legitimate legacy customization remains portable. The policy is precise enough for serious Gym history without making flexible productivity domains artificially rigid.
- Status: Accepted after domain, product, and adversarial-QA challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-029 — Legacy canonical repair requires exact paired-fact proof

- Context: An older automation path could write a Habit log whose canonical value was derived incorrectly for a custom unit. Rejecting every affected backup preserves bad analytics; blindly recomputing all generated rows risks rewriting intentional history or rows whose source fact no longer proves the conversion.
- Position A: Never repair; reject mismatched backups and leave live legacy rows unchanged.
- Position B: Recompute generated Habit canonical values whenever a current unit definition is available.
- Relevant evidence and constraints: Generated rows retain a paired metric entry with provenance, metric identity, raw value, unit, and canonical value. User-authored Habit history must never be rewritten. Unit definitions can change through versioning, and mere numeric similarity is not proof of original intent.
- Failure modes of Position A: Upgraded users retain a known calculation defect and portable backups fail despite recoverable evidence. Position B can alter unrelated or intentionally authored history and apply a newer unit contract to an older fact.
- Synthesis/decision: Apply a narrow proof-based repair only when the paired metric entry matches the full generated-row provenance, value, unit, metric, and canonical-conversion contract. Normalize qualifying portable backups during preflight and run the same awaited repair at startup while normal data access remains closed. Future generated writes take canonical truth from the metric entry and repair an existing exact generated row if replayed.
- Reason the selected design is superior for this application: It corrects the demonstrated legacy defect without general historical recomputation. Exact provenance turns repair into evidence-based reconciliation rather than a heuristic migration.
- Status: Accepted after domain and QA challenge; implemented in `IMP-20260901-019` and verified in `VER-20260901-021`.

### DEC-20260901-030 — External Task capture is bounded, ordered, and draft-safe

- Context: Android share targets can receive arbitrary `CharSequence` text and rapid `onNewIntent` deliveries. Whip retained the full extra in Activity saved state, interpreted every additional nonblank line as a subtask, and represented only one launch request. A large deliverable share could therefore fail during recreation, while a second share or widget Add Task could replace an open draft without a save/discard decision.
- Position A: Treat the newest platform intent as authoritative, immediately replace the editor, and report excess work with transient feedback. This is small and fast when only one intent arrives.
- Position B: Bound shares at the Activity boundary; retain accepted launch requests in a saveable FIFO; admit a new Task editor request only into a saveable conflict handoff; and retain a counted overflow fact until the user acknowledges that exact delivery/count.
- Relevant evidence and constraints: Android can reject an intent above Binder limits before Whip runs, so Whip controls only payloads it receives. `Intent.EXTRA_TEXT` is a `CharSequence`, emoji must not be split, and UTF-16 length is not a truthful character count. Activity recreation may occur before or after Compose admission. Widget/notification/deep-link actions must not inherit share-only capacity. A queued widget Add Task owns both its requested date and resolved creation Area even if saving the current draft changes visible Area scope.
- Failure modes of Position A: It silently loses authored drafts, later intents, or rejection notices; it can misdescribe a dropped notification as a share; and process death can erase a Snackbar before the user sees it. Position B can deadlock or replay unless consumption is exact-head-only, save/restore distinguishes an intentionally empty FIFO, conflicts provide Replace/Keep Editing, and acknowledgment binds both delivery ID and observed rejected-share count.
- Synthesis/decision: Select Position B. Bound raw share input to 8,192 code points and 32 KiB UTF-8, then retain a 200-code-point title plus at most 50 subtasks of 200 code points each. Normalize line endings and blank lines, never split a code point, and surface a persistent shortening warning. Keep accepted platform launches in an Activity-owned FIFO restored as an exact sequence; limit only waiting Task shares to four; collapse further shares into one counted overflow marker; never capacity-drop widget, notification, or deep-link actions. A saveable Task-editor handoff freezes capture text, shortening, date, and resolved Area and requires explicit Replace or Keep Editing. The overflow marker remains the FIFO head through recreation and is consumed only after acknowledgment of its exact delivery ID and current count.
- Reason the selected design is superior for this application: Share-to-Task remains fast for ordinary capture while large, rapid, mixed-source entry is deterministic and honest. Existing drafts and platform actions survive, partial imports are visible, Area/date ownership remains stable, and the architecture stays a narrow launch policy rather than a generic import framework.
- Status: Accepted after repeated platform, UX/accessibility, lifecycle, mixed-entry, and adversarial-size challenge; implemented in `IMP-20260901-020` and verified in `VER-20260901-022`.

### DEC-20260902-001 — Use a single-developer implementation loop

- Context: The initial maximum-quality instruction deliberately required extensive multi-agent audits, simulated focus groups, formal dialectics, and repeated adversarial approval rounds. That discovery phase produced a substantial evidence-backed backlog, but continuing the same orchestration for each implementation issue adds latency, context volume, and process overhead.
- Decision: The primary agent now owns reproduction, design judgment, implementation, focused regression coverage, proportional shared checks, emulator verification, concise durable memory, and commit/push. Work proceeds one coherent priority chunk at a time. No subagents, simulated agent panels, recursive audits, formal debates, or repeated approval rounds are used unless the user explicitly requests them.
- Rationale: The existing backlog and test infrastructure provide enough evidence to implement directly. This preserves correctness, compatibility, accessibility, durable memory, and revertible commits while returning the day-to-day workflow to normal software development.
- Scope: Historical audit records remain unchanged as evidence of work already performed. The simplified process overrides orchestration language in the reusable goal for all future work; it does not weaken product acceptance criteria or release gates.
- Related: `FB-20260902-001`, `MAXIMUM_QUALITY_GOAL.md`
- Status: Accepted by direct user instruction and active from 2026-09-02.

### DEC-20260902-002 — Habit timers use a durable canonical session ledger

- Context: A timestamp stored on `Habit` cannot safely own retry, unit conversion, reboot, restore, competing Start requests, or stale Stop actions, while historical duration must remain an immutable statement of what was logged.
- Decision: `habit_timer_sessions` is the authoritative timer ledger. Each Start has a stable request/session ID, one unresolved session per Habit, frozen Duration-unit identity, wall anchor for understandable recovery, monotonic anchor plus boot identity for exact running time, accumulated canonical seconds, and terminal Completed/Discarded tombstones. `Habit` retains only observable lightweight mirrors for UI/widget projection.
- Runtime policy: Running time and its display use monotonic elapsed time. Boot mismatch, missing monotonic identity, legacy migration, or portable restore becomes `ReviewRequired`; the user may correct and Stop & Log, Continue from a confirmed duration, or Discard. Exact Stop converts canonical seconds through the frozen unit and writes the Measurement, Habit log, terminal session, and cleared mirrors in one Room transaction. Competing Start request IDs are consumed so a delayed replay cannot create a surprise timer later.
- Compatibility: Schema 41 migrates every legacy active timestamp into review without creating or changing history. Portable backup format 18 carries unresolved timers only as review-required and removes device clock identity; private rollback preserves exact sessions; merge imports completed history but never a foreign active timer. Existing completed logs and metric entries are never recomputed.
- Rationale: This is narrower and more auditable than a generic timer engine, while providing the ownership and historical guarantees required by a productivity app that can be interrupted, restored, and controlled from widgets.
- Related: `FND-20260902-001`, `IMP-20260902-002`, `VER-20260902-002`.
- Status: Accepted and implemented.

### DEC-20260902-003 — Habit Today models action needed, finished, and unavailable as different states

- Context: Completion, skipping, pausing, and an off-schedule date have different historical meanings but all affect whether a user should act today. Treating them as one generic pending/done boolean makes Today counts and quick actions misleading.
- Decision: A completed or skipped Habit is finished for today's attention queue, while skip remains a distinct neutral historical outcome and never becomes completion. Paused and off-schedule Habits are unavailable for ordinary one-tap check-in and state that no check-in is expected. Intentional outside-schedule logging is exposed only as an explicit inspector action. An unresolved active timer overrides availability filtering so Stop/Review cannot disappear. User-visible time conversion uses `LocalWhipZone`.
- Rationale: The model stays narrow—no new persistence state or generic workflow engine—while matching what users need to decide at a glance. It prevents accidental history without forbidding deliberate exceptions and preserves truthful skip, completion, pause, and timer semantics.
- Compatibility: Existing Habit rows, schedules, pauses, skips, check-ins, timer sessions, history, Room schema 41, and backup format 18 are unchanged. The legacy `habit-done-disclosure` test tag remains stable while visible language becomes “Finished for Today.”
- Related: `FND-20260902-002`, `IMP-20260902-004`, `VER-20260902-004`.
- Status: Accepted and implemented.

### DEC-20260902-004 — Habit History follows effective dates and includes started neutral events

- Context: A creation/update timestamp answers when Whip wrote a row, while a Habit event's local date answers when the user's check-in, skip, or pause applied. History and Insights are user-facing accounts of the latter.
- Decision: Sort Habit history first by effective local date and only use write time/identity as a same-day tie breaker. Include check-ins, skips, and pauses whose start date is today or earlier in one editable “Habit History”; keep future pauses in Options until they begin. Paused/skipped dates are neutral authored events, not completion or failure. When no completed/missed period exists, Insights says “No scored periods.” Pause changes covering today/past explicitly disclose derived-stat recalculation while preserving check-ins and skips.
- Rationale: This produces the chronology users mean without changing persistence or inventing historical snapshots. It keeps future planning separate from occurred history, makes neutral states explainable, and warns precisely where a schedule edit can alter derived interpretation.
- Compatibility: Existing check-ins, skips, pauses, timestamps, Room schema 41, backup format 18, and calculation rules are unchanged. Presentation is derived from existing immutable facts; no row is migrated or recomputed in storage.
- Related: `FND-20260902-003`, `IMP-20260902-005`, `VER-20260902-005`.
- Status: Accepted and implemented.

### DEC-20260902-005 — Enlarged text preserves named direct navigation; clear Home preserves context

- Context: Six short primary destinations can fit one phone row at some enlarged scales but not all. A fixed threshold discarded all visible names at 150%, while a fixed rail width clipped them. Home also treated “nothing due” as sufficient even when existing work was merely outside the dashboard.
- Decision: Measure rendered destination labels against the actual compact width. Use one stable named row when every label fits and two stable three-item named rows otherwise; never fall back to icon-only primary navigation. Size the rail from its rendered names and make its direct list vertically scrollable in short windows. On a settled, clear returning Home, show at most three concrete recovery routes ordered Inbox → Upcoming → Habits → Goals → Tracks → Gym, while recent completion evidence continues to route to Review & Trends.
- Rationale: The interface adapts to real content instead of an arbitrary font threshold, retains one-tap/muscle-memory access, and bounds Home assistance without becoming a guilt dashboard or a second navigation system.
- Compatibility: App-destination order, saved workspace state, Back behavior, deep links, keyboard shortcuts, Room schema 41, backup format 18, and every domain record remain unchanged. Recovery links only select existing workspace destinations.
- Related: `FND-20260831-011`, `FND-20260831-017`, `FND-20260902-004`, `IMP-20260902-006`, `VER-20260902-006`.
- Status: Accepted and implemented.

### DEC-20260902-006 — Global search owns cross-Track discovery; local search owns collection filtering

- Context: Whip already had a complete global index and exact result router for Track definitions and Entry content, while a second Track-list query existed only as unreachable state. Adding another visible text field would duplicate scope, conflict with reorder behavior, and place two search owners in the same compact workspace.
- Decision: Use the persistent workspace search as the single cross-Track owner and name its exact live scope in the accessibility action, placeholder, scope summary, and empty guidance. Keep Activity search for filtering the visible chronological feed and per-Track Entry search for one Track's history. Remove the unreachable Track-list query branches. Route an archived Track result to the Archived destination and wait for the scoped projection before consuming a cross-Area request.
- Rationale: Users get one predictable entry point that is more capable than the dead branch, while legitimately different local searches remain close to the collections they filter. The design reduces state and contradictory controls without removing any reachable behavior.
- Compatibility: Search indexing, Entry FTS, Track/Entry identities, Area scope, saved workspace state, Room schema 41, backup format 18, and all history remain unchanged. Only action labeling, result presentation/navigation, and unreachable UI state changed.
- Related: `FND-20260831-018`, `FND-20260902-005`, `IMP-20260902-007`, `VER-20260902-007`.
- Status: Accepted and implemented.

### DEC-20260902-007 — Test real viewport contracts on valid profiles and preserve semantic parity across layouts

- Context: A 320dp physical root cannot render a synthetic 360dp or desktop-width host merely because a test requests it. Conversely, a clipped result under a real API 26 keyboard is a production defect, not a test inconvenience. Wide master/detail layouts may intentionally present the selected identity in more than one semantic region.
- Decision: Validate compact behavior on a physically compact API 26 profile, adaptive/fold/wide contracts on an API 37 display large enough to host them, and the complete product gate on the representative API 34 phone. Tests derive physical edges from the root, scroll through the owning collection before interacting, branch only on genuine compact-versus-persistent navigation, and scope intentional wide-pane duplicates to the relevant owner. Search disclosure changes reset to the state-summary region; emoji search prioritizes the result above custom creation while the keyboard is open.
- Rationale: This keeps production geometry honest, prevents false failures caused by impossible canvases, and still treats every defect reproducible in a valid supported viewport as a product bug. Semantic parity matters more than forcing compact and wide compositions to expose identical node counts or simultaneous visibility.
- Compatibility: Presentation, focus, keyboard, and test targeting only. No Room schema, backup format, search index, identity, workout/program rule, saved record, or historical fact changed.
- Related: `FND-20260902-006`, `IMP-20260902-008`, `VER-20260902-008`.
- Status: Accepted and implemented.

### DEC-20260902-008 — Area changes use one request-owned result with repository-authoritative truth

- Context: Areas are shared ownership metadata for Tasks, Habits, Goals, Tracks, Entries, saved scopes, and widgets. Their apparently small controls can therefore trigger cross-domain moves, deletions, and Settings reconciliation. Optimistically closing a dialog before that work finishes loses authorship and cannot distinguish a rejected transaction from a committed transaction whose derived cleanup failed.
- Decision: Serialize Area-management mutations through one ViewModel-owned request state and return a typed receipt naming the exact operation, source identity, optional destination identity, and committed follow-up warnings. The initiating surface retains its draft/choice, disables dismissal and duplicate submission only while saving, consumes only its matching result, and closes on authoritative success. Repository/database operations remain the source of truth; destructive methods do not depend on an asynchronously collected UI projection. Read-modify-write color changes are transactional. Archived-name creation restores the same Area and preserves its color; archive, restore, search, and conflict copy all model archived state explicitly.
- Rationale: This gives consequential cross-domain operations the same lifecycle and retry truth as other authored Whip changes without forcing harmless pickers or immediate preferences into a generic transaction DSL. A committed warning cannot trigger an unsafe retry, while a pre-commit failure leaves the user's intent intact and understandable.
- Compatibility: No Room schema, migration, backup format, Area ID, assignment, historical record, or existing saved color is rewritten. Existing archived Area identity is reused on restoration. Atomic deletion and assignment behavior remains in the existing repositories/coordinators.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-007`, `IMP-20260902-009`, `VER-20260902-009`.
- Status: Accepted and implemented.

### DEC-20260902-009 — Tags use explicit global operations and a stable archived lifecycle

- Context: Tags are shared labels whose canonical registry row is keyed by ID while Task, Habit, Goal, and Track references are intentionally denormalized names. That makes a rename or merge cross-domain, while archive should hide a reusable choice without rewriting saved items.
- Decision: Keep the existing schema, but define separate transactional operations: Rename changes one Tag’s spelling and every matching reference; Merge replaces the source spelling with one active destination across all four domains and then removes only the source row; Archive changes only registry visibility and preserves every item reference. Startup/save reconciliation may ensure that a referenced label exists but cannot silently restore an archived Tag. Explicit Create/Restore may reactivate the same identity. Commas remain reserved by current CSV persistence and are rejected in both domain and UI. The manager owns create, rename, merge, archive, restore, search, usage disclosure, and exact request receipts.
- Rationale: The design fixes demonstrated correctness and usability failures without migrating all four product domains to a speculative tag-link abstraction. Archived state becomes durable and predictable, global operations are falsifiable, and users can see consequences before acting while ordinary item editors remain lightweight.
- Compatibility: No Room schema or backup-format change. Existing Tag IDs and item records are retained. Archive never rewrites references; explicit Rename/Merge intentionally updates Tag labels on saved Tasks, Habits, Goals, and Tracks while preserving their identities and other history.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-008`, `IMP-20260902-010`, `VER-20260902-010`.
- Status: Accepted and implemented.

### DEC-20260902-010 — Workout deletion removes the selected fact graph and preserves programming decisions

- Context: A completed Workout owns placements, groups, and Sets, but it is also referenced by reconstructible personal records and immutable 5/3/1 Training Max decisions. Gym History previously confirmed against asynchronously collected UI projections and closed before persistence reported an outcome.
- Decision: Review and commit permanent Workout deletion through one transaction-derived `WorkoutDeletionImpact` and revision token. Delete only the selected session graph; reject active sessions and changed reviews. Rebuild personal records after commit, apply the retired-Link compatibility policy without retracting its audit rows, cancel the timer afterward, and classify failures in those post-commit follow-ups as warnings. Preserve Training Max decisions, Goal contributions, generated Habit check-ins, automation occurrences, Exercise definitions, and Routine templates as historical facts. Keep the dialog/request owned until exact success, failure, or read-only recovery verification.
- Rationale: Historical workout Sets are authoritative performed facts, personal records and timers are reconstructible projections, retired Link/automation rows are immutable audit history, and Training Max decisions explain why later programming changed. This boundary prevents stale destructive confirmation, avoids false retries after commit, and keeps 5/3/1 and automation history truthful without adding a new schema or generic programming DSL.
- Compatibility: No Room schema, migration, or backup-format change. Only the explicitly selected completed Workout graph is removed. Existing Training Max decisions and linked historical facts remain; derived records are reconciled from surviving workout data.
- Related: `FND-20260831-019`, `FND-20260901-025`, `FND-20260902-009`, `IMP-20260902-011`, `VER-20260902-011`.
- Status: Accepted, implemented, and fully verified.

### DEC-20260902-011 — Machine deletion uses the shared exact Gym request lifecycle

- Context: Machine deletion already had a transaction-derived impact and revision check, but its presentation result was not owned across recreation. Completed workout equipment snapshots are historical facts while routine references are current editable definitions.
- Decision: Represent Machine deletion as `GymDeletionKind.Machine` in the same bounded request lifecycle used by other exact Gym deletions. Save the target ID, UUID, data generation, reviewed impact, and revision; reject UUID or revision mismatch; serialize one owner; and on process recovery read current repository truth. An absent exact target settles achieved once, while a present or unverified target requires retry/review. Preserve the existing transaction semantics: block active use, delete only the selected Machine, mark affected routines as needing equipment, and retain completed-workout snapshots.
- Rationale: One lifecycle gives every consequential Gym deletion the same authorship, replay, and recovery guarantees without duplicating a coordinator or inferring deletion from asynchronously collected lists. It preserves history while making uncertainty explicit.
- Rejected alternatives: Callback-only success because it is lost across lifecycle changes; list-based absence inference because projections may be stale; and a separate Machine-only coordinator because it would duplicate exact-deletion state and drift.
- Compatibility: No Room schema, migration, backup-format, completed-set, workout snapshot, Exercise, or Routine identity change. Only the reviewed Machine profile is removed; existing affected routines retain identity and are marked for repair.
- Related: `FND-20260902-010`, `IMP-20260902-012`, `VER-20260902-012`.
- Status: Accepted, implemented, and fully verified.

### DEC-20260902-012 — Adopt bounded VERA routing without restoring recursive Whip process

- Context: The provided VERA bundle defines cheap-first Luna/Terra/Sol routing, explicit risk gates, bounded retries, and deterministic stopping. The user separately directed Whip back to a conventional primary-developer process with no recursive panels or unrequested subagents.
- Decision: Install VERA globally and in Whip, including all five roles, a three-agent cap, one same-tier repair, deterministic verification order, and selective Sol escalation. Global defaults become Terra/medium for new sessions. Whip retains a repository-level override: roles are available, but delegation occurs only when the user explicitly requests it. Preserve all existing global notify, MCP, plugin, trust, desktop, approval, and sandbox settings.
- Rationale: This makes the requested agent system available and reproducible while honoring the more specific Whip workflow preference and preventing the unbounded debate/retry behavior the user rejected.
- Compatibility: No application source, build, persistence, user data, release artifact, or device state changes. The current already-running Codex session keeps its existing model; new sessions load the new defaults.
- Related: `FB-20260902-001`, `FB-20260902-002`, `IMP-20260902-013`, `VER-20260902-013`.
- Status: Superseded by `DEC-20260902-013`; the user explicitly rejected the merged Whip override and requested canonical VERA only.

### DEC-20260902-013 — Canonical VERA is the sole orchestration policy

- Context: The initial installation preserved VERA's mechanics but paraphrased its policies and layered ATIS globally plus a Whip-specific no-unrequested-delegation rule. The user explicitly requested a clean orchestration slate and an independent VERA repository.
- Decision: Treat `commvnist/vera-codex` as the canonical source. Install its `AGENTS.md`, `.codex/config.toml`, five custom agent definitions, and `routing-policy.yaml` without content changes in Whip. Globally, install the same AGENTS/routing/role contents and exact VERA model/agent values while preserving unrelated machine-local integrations. Remove `atis-fast-explorer.toml`, all ATIS instructions, and the Whip conventional-development adaptation. Do not retain another agent methodology in the active instruction chain.
- Rationale: Archive parity makes the installation falsifiable and eliminates ambiguity about whether VERA or a locally synthesized hybrid governs orchestration. Preserving unrelated integrations avoids conflating an agent-policy reset with destructive loss of authentication, plugins, MCP servers, notifications, project trust, or desktop preferences.
- Compatibility: No Whip application source, schema, data, build artifact, or device state changes. Historical product-memory records remain as an audit trail but are not active Codex instruction sources. New Codex sessions load the reset instruction chain; this already-running session retains the instructions it began with.
- Related: `FB-20260902-004`, `IMP-20260902-014`, `VER-20260902-015`.
- Status: Accepted and implemented.

### DEC-20260902-014 — Give every physical release a unique upgrade identity

- Context: The latest source is newer than the phone-verified `0.3.34` release, but the build metadata still reused version code 40 and version name 0.3.34. Reinstalling materially different code under the same identity would make upgrade diagnosis, rollback tracking, and installed-artifact evidence ambiguous.
- Decision: Release the current candidate as version `0.3.35` with monotonically increasing version code 41. Require the full release gate and signing verification before a data-preserving `adb install -r`; verify the exact endpoint is a physical device and capture installed version, signer, hash, and `firstInstallTime` before and after installation. Do not clear application data or run instrumentation on the phone.
- Rationale: A unique release identity makes support and provenance falsifiable while retaining Android's normal in-place upgrade and Room migration path for existing users.
- Compatibility: The version change does not alter application data. The candidate includes Room schema 41 and its explicit migrations; previously completed records remain governed by their existing compatibility guarantees.
- Related: `FB-20260902-005`, `IMP-20260902-015`.
- Status: Accepted for the requested physical-device release.

### DEC-20260902-015 — Expand 5/3/1 through generated, editable program structure

- Context: Whip already persists typed phases, Training Max boundaries, work sections, and immutable workout prescriptions, but new-program setup still hardcodes one four-week cycle, one same-lift Supplemental block, and one ungated Joker candidate. Book-level Leader/Anchor structures, exact 7th Week uses, alternate-lift BBB, Joker ladders, and balanced assistance would otherwise require repetitive manual editing or misleading generic placements.
- Decision: Add editable 11-week Leader/Anchor plans generated from typed phase specifications: two three-week 5s PRO Leaders, a 7th Week transition, one three-week PR-set/FSL Anchor, and a closing 7th Week protocol. Use standard 5/3/1 weekly percentages and explicit Deload, TM Test, and PR Test matrices. Advance Training Maxes after Leader 1 and after each completed 7th Week boundary. For schedules that repeat a main lift, persist synchronized non-test protocol templates on every occurrence, retain one explicit TM-test owner, and select deterministic balanced runtime owners so each logical lift executes once while the available training days remain useful. Represent alternate BBB with a dedicated Supplemental placement using the selected alternate lift's own Training Max. Represent one to three Jokers as ordered Optional rows and expose each next row only after successful prerequisite work; a skip, failure, RPE 9+, or RIR 1 or lower ends the ladder. Build optional Push/Pull/Single-leg-Core assistance drafts only from compatible active exercises already in the user's library, with every choice visible and replaceable before save.
- Truthfulness boundary: Public Wendler material establishes the concepts and same-lift/alternate-lift BBB, but does not publish every numeric book prescription. UI calls these “5/3/1 Forever structure” and “book-guided editable”, displays every generated percentage, and tells lifters to verify against the edition/template they follow rather than claiming an official exact template.
- Rationale: Generated sets and existing phase metadata are already the durable domain representation, so no Room or backup migration is necessary. A single new string-backed Supplemental placement kind fixes alternate-lift semantics without a programming DSL. Existing routines are not recomputed; completed sessions retain their original snapshots.
- Compatibility: Once-per-lift runtime protocol ownership requires a phase-specific once-per-lift role plus recognized 5/3/1 template revision 2 or later. Legacy revision-1 Beginners deloads keep every saved repeated exposure; explicitly applying a new protocol upgrades the routine's durable template provenance but opts in only that edited phase. A second untouched legacy protocol phase retains its base role and saved runtime behavior. Template revisions otherwise apply only to newly built or explicitly replaced drafts. Eligibility continues to derive solely from Main work, while Training Max changes synchronize to Supplemental placements for the same exercise. Existing Boolean Joker snapshots remain a correct summary of whether any Joker rows exist.
- Related: `FB-20260902-006`.
- Status: Accepted, implemented, and independently approved after the VERA architecture gates.

### DEC-20260902-016 — Primary editors share one responsive chrome contract

- Context: Full-screen authoring surfaces had independently evolved title, dismissal, navigation, divider, and commit controls, producing inconsistent hierarchy and fragile compact layouts.
- Decision: Use `WhipEditorHeader` for primary Task, Habit, Goal, Track, Track Entry, Routine, Machine, Exercise, tracked-record, and Set editors. The title and one exit/up action own the first row; the filled commit action stays visually primary and moves to a trailing second row when the available width cannot support enlarged text. Routine child pages use Back to the outline; the outline uses one X exit and no duplicate Back/Close actions.
- Rationale: One small shared primitive fixes the visible cross-product inconsistency without coupling form bodies or persistence rules to a generic editor framework. Width-and-font-scale adaptation preserves identity and actions rather than hiding either.
- Compatibility: Presentation only; no domain, Room, backup, routine, workout, or historical data changes.
- Related: `FND-20260902-011`, `IMP-20260902-019`, `VER-20260902-020`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-019 — Bound in-memory search before introducing a persistent index

- Context: Unified Search already builds off-main and caps each domain, but per-entity newest-history selection still performed a full sort. A persistent full-text/index schema would add synchronization and migration complexity without evidence that the bounded in-memory source set is insufficient.
- Decision: Select the newest N values with a stable bounded priority queue: one selector evaluation per input, O(N) retained memory, O(total log N) work, descending output, and original-order stability for equal timestamps. Keep the existing independent 2,000-result domain cap and explicit limited-source state. Add deterministic 10,000-value and 10,000-results-per-domain contracts without flaky wall-clock thresholds.
- Rationale: This removes the demonstrated unbounded intermediate work while retaining simple, inspectable search semantics. Operation-count contracts catch algorithmic regressions on any CI host; a persistent index remains available if real measurements later justify it.
- Compatibility: Search projection only; no schema, backup, query syntax, result identity, navigation, or historical data changes.
- Related: `FND-20260902-014`, `IMP-20260902-022`, `VER-20260902-023`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-017 — Cross the breaking persistence boundary with a durable data epoch

- Context: The user explicitly authorized an update-time local-data wipe so Whip could stop carrying migratory and compatibility architecture. Room destructive fallback alone cannot coordinate preferences, restore journals, widgets, work, notifications, or stale launch requests and cannot provide an authored confirmation boundary.
- Decision: Make schema 42 and portable-backup envelope 3/data version 19/data epoch 2 the only accepted contracts. Before any Whip database or recovery access, resolve an `AtomicFile` epoch marker from no-backup storage. Existing Whip state without the current marker enters a dedicated two-step reset screen; a durable `ResetInProgress` marker precedes all deletion; an interrupted or confirmed failed reset remains blocked and resumes under one mutex. Clear enumerated Whip preferences and files, cancel work/notifications, recreate and verify schema 42/defaults/a fresh generation, invalidate widgets and launch requests, then mark Current and start normal runtime. Never use Room destructive fallback or accept an old backup.
- Rationale: A durable gate makes the intentional incompatibility visible and recoverable across process death while eliminating the false promise that obsolete shapes remain supported. Exact-current backups and one schema drastically reduce conditional architecture without silently recomputing history.
- Safety boundary: Epoch-inspection failures can only recheck or close; they cannot authorize reset. Rapid retries are state-claimed and mutex-serialized. Non-Whip/platform preferences and external user-owned backup documents are left intact.
- Compatibility: Deliberately breaking. Updating users must explicitly erase local Whip data before entering schema 42; pre-epoch backups are rejected. This is the requested clean slate, not a data-preserving migration.
- Related: `FND-20260902-012`, `IMP-20260902-020`, `VER-20260902-021`.
- Status: Accepted, implemented, and targeted/emulator verified.

### DEC-20260902-018 — Separate persisted enum identity from deliberate interface language

- Context: Kotlin enum names were convenient stable identifiers, but several were also rendered as UI copy. That coupled persistence/programming names to capitalization, spacing, localization, and product terminology.
- Decision: Give every audited user-visible enum an explicit `label` (or purpose-specific `periodLabel`) while continuing to serialize by enum `name` where the current schema requires it. Render localized weekdays through `DayOfWeek.getDisplayName`. Reuse `EditorSectionHeader` for Settings and Gym section hierarchy instead of repeating divider/type/spacing recipes.
- Rationale: Explicit labels allow “In progress”, “All Habits”, “Exercise sessions”, and similar product language without changing stored identities. One section primitive makes hierarchy consistent while keeping each screen's content independent.
- Compatibility: Presentation/model metadata only. Enum constants and storage names remain unchanged; no schema, backup, existing setting, routine, workout, or history is rewritten.
- Related: `FND-20260902-013`, `IMP-20260902-021`, `VER-20260902-022`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-020 — Give repeated controls intent-specific semantics

- Context: A repeated visible label can be clear within a visual column while remaining ambiguous to accessibility services and automation once the same action appears for several lifts, fields, or work sections.
- Decision: Shared selection options announce `field label + option value`; repeated 5/3/1 Training Max mode controls expose lift-specific descriptions and stable lift-role tags; workout-only exercise actions are scoped to a named active-workout empty-state region. Retain ordinal selectors only for noninteractive duplicate display assertions where order itself is the contract.
- Rationale: Contextual semantics improve actual assistive use and make regression tests describe intent. They are more durable than globally unique copy or layout-position assumptions and do not add visible noise.
- Compatibility: Semantics and tests only; no schema, backup, routine, prescription, workout, or historical-data changes.
- Related: `FND-20260902-015`, `IMP-20260902-023`, `VER-20260902-024`.
- Status: Accepted, implemented, and targeted-test verified.

### DEC-20260902-021 — Let every consequential secondary dialog own its exact result

- Context: The broad secondary-mutation campaign had one remaining authored draft (Exercise Category) and one destructive graph (Track) that still closed on dispatch rather than authoritative completion.
- Decision: Give Category saving a dedicated request state and dialog-owned coordinator. For Track deletion, build the definition/history/value/Link/automation impact inside one transaction, hash that graph into the reviewed revision, require the same revision at commit, publish one request-owned receipt, and downgrade only post-commit reconciliation failure to a warning. Preserve lightweight synchronous picker and local-preference actions where failure cannot abandon a draft or create an ambiguous historical outcome.
- Rationale: This completes the product-wide interaction rule without imposing a generic workflow engine on harmless controls. Users keep context exactly where retry matters, and permanent deletion becomes falsifiable and safe from stale review or false replay.
- Compatibility: No schema or backup-format change. Category data changes only after a successful authored save; Track deletion remains explicitly permanent but now binds the exact reviewed graph.
- Related: `FND-20260831-019`, `FND-20260901-027`, `FND-20260902-016`, `IMP-20260902-024`, `VER-20260902-025`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-022 — Use one typed request channel for Gym catalog authoring

- Context: Category saving already survived recreation, but Machine/Exercise editors independently mixed local `saving` flags with callbacks owned by a particular composition.
- Decision: Route Category, Machine, Machine-version, Exercise, and nested Exercise-for-Machine persistence through one mutex-serialized ViewModel request state with typed receipts. Compose coordinators reclaim only their namespaced request, retain drafts and inline failures, and close the precise editor layer after success. Extract catalog overlays from the Gym shell so the ownership logic remains testable and coverage instrumentation stays below JVM method limits.
- Rationale: The catalog permits only one foreground authoring operation, so one typed channel is smaller and clearer than five ad hoc callback protocols. Typed outcomes distinguish closing a whole editor stack from returning a newly created Exercise to the underlying Machine editor.
- Compatibility: No schema, backup, catalog identity, Routine definition, active Workout, or completed History change.
- Related: `FND-20260902-017`, `IMP-20260902-025`, `VER-20260902-026`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-023 — Make schema 43 the canonical structured-Gym model

- Context: The user authorized a breaking clean slate specifically to remove migratory and legacy behavior. Schema 42 still carried old program-kind aliases and a duplicated assistance-role representation even though current 5/3/1 generation already expresses Classic/5s PRO and BBB/FSL/SSL/BBS as independent executable policies.
- Decision: Persist only `Static`, `Custom`, or canonical `FiveThreeOne` program identity. Persist work structure only as `placementKind` plus `assistanceCategory`; keep the assistance-role picker as transient builder state. Require applied Training Max values to declare the `Explicit` source and require every Joker candidate, including a single Joker, to follow the ordered 5-point ladder above the final Main percentage. Cross a new explicit fresh-start boundary at Room schema 43, data epoch 3, and portable-backup data version 20; retain only the current schema and accept no migration or older backup.
- Rationale: Main work, supplemental work, optional work, and assistance are orthogonal program concepts, not alternate program identities. One source of truth prevents impossible combinations while retaining full customization. A new epoch is safer and more truthful than pretending a schema-shape removal is compatible.
- Compatibility: Deliberately breaking as previously authorized. Updating installations must confirm the fresh-start gate; historical schema 42 and portable-backup version 19 are rejected rather than transformed. No released history is silently recomputed.
- Supersedes: The current-boundary values in `DEC-20260902-017`; that decision remains historical evidence for the preceding clean-slate step.
- Related: `FND-20260902-018`, `IMP-20260902-026`, `VER-20260902-027`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-024 — Keep current-only Settings and direct users to the first invalid field

- Context: The corrected Settings targeted profile surfaced both leftover preference migration behavior and a Custom Unit form whose inline error could be created outside the visible viewport.
- Decision: Treat stored Settings as current-epoch data only: ignore unknown old keys, stop deleting them as a migration side effect, and derive Health Connect categories only from the explicit current category set. For Custom Unit validation, retain the enabled explanatory action but focus and bring the first invalid Name or conversion-factor field into view. Keep the destructive epoch-reset integration test in an isolated emulator batch rather than mixing it with lifecycle/UI tests.
- Rationale: The data-epoch gate owns destructive upgrade behavior; repositories should not carry a second hidden compatibility system. Error-directed focus preserves clear validation without disabling an action whose purpose is to explain what is missing. Isolated destructive QA is both faster and deterministic.
- Compatibility: Intentionally current-only under the authorized clean reset. No current setting name/value changes; no Room or backup change beyond schema 43. Presentation behavior changes only after an invalid Custom Unit submission.
- Related: `FND-20260902-019`, `FND-20260902-020`, `IMP-20260902-027`, `VER-20260902-028`.
- Status: Accepted, implemented, and targeted emulator verified.

### DEC-20260902-025 — Locale-sensitive composables read observable configuration

- Context: The design-consistency pass intentionally localized weekday labels, but one Habit schedule path read the Java process default directly during composition.
- Decision: Capture the current Android configuration locale in the owning composable and pass that value into weekday label functions. Apply the same explicit observable locale to the corresponding Settings weekday selectors.
- Rationale: Configuration is Compose-observable and preserves the explicit-label contract across runtime language changes without moving presentation concerns into persistence identifiers.
- Compatibility: Presentation-only; no setting, Habit schedule, stored weekday, database, or backup changes.
- Related: `FND-20260902-021`, `IMP-20260902-028`, `VER-20260902-029`.
- Status: Accepted, implemented, and lint verified.

### DEC-20260902-026 — Recovery operation completion includes terminal-state publication

- Context: The core recovery gate updates its own state before returning, while Whip's application-facing aggregate copied that state asynchronously for pre-gate/fresh-start support.
- Decision: In addition to continuous collection, copy the underlying gate's current state in `finally` after replace restore and exclusive reset, directly after pending-recovery blocking, and after retry settles. The gate remains the authority; the application aggregate cannot lag beyond the operation boundary.
- Rationale: This preserves fail-closed access during transitions while giving callers a deterministic postcondition. Waiting or polling in every consumer would spread lifecycle races through UI, workers, widgets, and tests.
- Compatibility: Runtime synchronization only; no persisted data, schema, backup format, or recovery decision changes.
- Related: `FND-20260902-022`, `IMP-20260902-028`, `VER-20260902-029`.
- Status: Accepted, implemented, and repeated emulator verified.

### DEC-20260902-027 — Keep navigation identity independent from interface copy

- Context: `DestinationTabBar` supports an explicit stable tag value, but Habit navigation relied on the default visible label. The label became “All Habits” while the durable enum identity remained `All`.
- Decision: Habit destinations use `HabitDestination.name` for test identity and `HabitDestination.label` for visible and spoken interface language in both loading and normal workspace states.
- Rationale: Product copy should optimize comprehension and remain free to localize; automation identity should optimize stability. Keeping those concerns explicit preserves fast, meaningful regression coverage without compromising interface language.
- Compatibility: Semantics/test identity only. No Habit, schedule, log, setting, database, or backup data changes.
- Related: `FND-20260902-023`, `IMP-20260902-029`, `VER-20260902-030`.
- Status: Accepted, implemented, and repeatedly emulator verified.

### DEC-20260903-001 — Release the breaking epoch without silently erasing phone data

- Context: Version 0.3.37/code 43 intentionally accepts only Room schema 43, data epoch 3, and backup version 20. The connected phone still held the released 0.3.35/code 41 installation and older local-data epoch.
- Decision: Upgrade the signed package in place with `adb install -r`, cold-launch it, and verify that the explicit two-step fresh-start boundary is presented. Do not invoke the in-app erase action as part of deployment or automated smoke verification.
- Rationale: Android package identity and the established signer remain continuous while the product truthfully requires direct user confirmation before the authorized breaking local reset. This keeps deployment and destructive data erasure distinct and auditable.
- Compatibility: The installed application is upgraded, but existing Whip local data remains untouched until the user confirms “Erase all Whip data”. Older backups remain intentionally unsupported by the current epoch.
- Related: `FB-20260903-001`, `IMP-20260903-001`, `VER-20260903-001`.
- Status: Accepted, released, and physically verified.

### DEC-20260903-002 — Existing-routine edit projections copy persisted program policy

- Context: The persistence model retained Performance review, but the edit adapter reconstructed a partial program draft and allowed defaults intended for new programs to replace two omitted persisted fields.
- Decision: Treat the saved routine as authoritative when entering edit mode and explicitly project `progressionMode` and `allowNonStandardHigherSuggestions` alongside all other program metadata. Protect the projection boundary with a round-trip regression fixture using non-default values.
- Rationale: Default constructor values are appropriate for creation, not hydration. An explicit complete projection prevents the editor from visually or durably changing authored program policy.
- Compatibility: No schema, backup, historical Workout, or calculation change. Existing correctly persisted routines immediately reopen with their actual configuration after updating.
- Related: `FB-20260903-002`, `FND-20260903-001`, `IMP-20260903-002`, `VER-20260903-002`.
- Status: Accepted, implemented, verified, and released in 0.3.38/code 44.

### DEC-20260903-003 — Make the Routine edit projection lossless for complete per-lift state

- Context: The program-level projection repair exposed the same default-substitution risk in four `RoutineExerciseDraft` fields that are consumed and persisted by the builder but were absent from its UI projection.
- Decision: Copy Training Max basis kind, basis value, basis unit, and increase eligibility directly from every saved Routine placement. Extend the existing advanced-programming round-trip regression with non-default values for each field.
- Rationale: The edit draft is a complete replacement contract for child placements. Preserving these fields at its only UI hydration boundary is smaller and safer than repository exceptions that guess whether a default was intentional.
- Compatibility: No schema, backup, completed Workout, Training Max calculation, or current-cycle change. Existing routines retain their already-stored values after updating.
- Related: `FB-20260903-003`, `FND-20260903-002`, `IMP-20260903-003`, `VER-20260903-003`.
- Status: Accepted, implemented, verified, and released in 0.3.38/code 44.

### DEC-20260903-004 — Derive Gym summaries from eligible immutable history

- Context: The Gym-wide follow-up found that record eligibility, machine-setting direction, copied-session state, routine-day references, graph-preset validity, and weekly attribution were enforced in different layers or not enforced at all.
- Decision: Centralize record eligibility in the rebuild transaction using the workout placement’s immutable exercise-policy snapshot plus current warm-up/assisted preferences and session inclusion. Store numbered-machine direction in every workout placement and use that snapshot for PR reconstruction and deleted-machine graph fallback. Reset every timer/progression-invalidity field when repeating a workout, remove the unused invalid “finished copy” mode, clear routine-day numeric references before replacing/deleting their rows, validate graph presets atomically, and count weekly records by included source-session identity. Establish current-only Room schema 44, data epoch 4, and backup data version 21 rather than migrating incomplete historical snapshots.
- Rationale: Completed history must remain self-describing, while current user preferences should control whether that history participates in derived records. Session identity/local date is a safer attribution boundary than UTC timestamps, and rejecting malformed authored data is safer than downstream fallback.
- Compatibility: Intentionally breaking under the user-authorized clean slate. Older local data/backups are rejected through the explicit existing reset boundary; the release process installs in place but never confirms erasure for the user.
- Related: `FB-20260903-004`, `FND-20260903-003`, `FND-20260903-004`, `FND-20260903-005`, `IMP-20260903-005`, `VER-20260903-005`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.39/code 45.

### DEC-20260903-005 — Enforce semantic validity at each durable cross-feature boundary

- Context: The post-Gym audit found a repeated pattern outside Gym: presentation paths prevented some invalid inputs, while repositories, notification actions, taxonomy operations, settings, and backup restore could still create ambiguous or cross-owned state.
- Decision: Make repositories authoritative for authored constraints; preserve referenced history through archival rather than deletion; model connected Habit progress and focus timers as ownership-bearing states; update dependent revision/search projections in the same transaction as taxonomy changes; normalize measurement identities before lookup; and require whole-snapshot semantic validation before backup preview, merge, or restore. Serialize read-modify-write mutations that can be reached concurrently.
- Rationale: Every mutation path then observes one falsifiable contract, historical facts remain interpretable, derived projections match canonical rows, and invalid portable data is rejected before partial durable effects. This is targeted hardening of existing domain concepts rather than a new generic framework.
- Compatibility: No new Room schema or backup version was required beyond schema 44/data epoch 4/backup 21. Invalid newly authored or restored data is now rejected; valid current data round-trips unchanged. Checklist definitions referenced by history remain as archived rows.
- Related: `FB-20260903-005`, `FND-20260903-006` through `FND-20260903-009`, `IMP-20260903-007`, `VER-20260903-007`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.40/code 46.

### DEC-20260903-006 — Use a guided full-pane 5/3/1 setup with Program Structure as the edit authority

- Context: 5/3/1 creation has substantially more hierarchy and validation than an ordinary exercise edit, but lifters still need fast arbitrary-lift customization and familiar routine editing after generation.
- Decision: Keep the hybrid model: a dedicated full-pane guided 5/3/1 setup produces the initial structured routine, while Program Structure remains the authoritative place for later program-wide Training Max, phase, progression, and prescription edits. Reuse the shared searchable exercise picker rather than introducing another lift selector. Suppress generic placement rewrite tools only for program-controlled Main/Supplemental work; keep explicit advanced set editing and ordinary-routine flexibility intact.
- Rationale: This preserves a clear novice path and efficient expert customization without locking 5/3/1 to four named lifts, duplicating library UX, or building a generic programming DSL. Central edit routing makes semantic ownership visible and reduces accidental prescription loss.
- Rejected alternatives: Refining the compact alert still constrains a program-sized task; putting every program option directly into ordinary placement editing scatters ownership and conditional logic; disabling all set-level edits would unnecessarily remove expert control.
- Compatibility: No persistence or schema change. Existing routines and completed workouts retain their exact stored structure; only authoring presentation and access to unsafe bulk helpers changes.
- Related: `FB-20260903-006`, `FND-20260903-010` through `FND-20260903-012`, `IMP-20260903-009`, `VER-20260903-009`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.41/code 47.

### DEC-20260903-007 — Treat search and contextual creation as one reusable Gym picker contract

- Context: Selecting an existing Exercise and creating a missing one are two outcomes of the same user intent. Separate or passive empty-state behavior caused inconsistent return paths, repeated typing, and a custom 5/3/1 dead end.
- Decision: The shared single-select Gym picker owns search, visible results, contextual noun labels, a permanent Create action, and a query-specific empty-state Create action. It passes the normalized query to the shared Exercise editor and leaves persistence/return ownership with the invoking workout or program workflow. Multi-select Routine and Machine pickers retain their specialized selection UI but follow the same search/always-create/actionable-empty-state contract. Custom 5/3/1 models “add next lift” as an open slot rather than requiring an existing unused library row.
- Rationale: This gives every Gym entry point the same mental model without forcing different single- and multi-select tasks into one oversized component. The open-slot model supports unlimited successive creation while preserving distinct per-lift Training Max state.
- Compatibility: UI/state-flow only; no persistence or schema change. Existing Exercises, Routines, active workouts, and historical workouts are unchanged.
- Related: `FB-20260903-007`, `FND-20260903-013`, `FND-20260903-014`, `IMP-20260903-011`, `VER-20260903-011`.
- Status: Accepted, implemented, verified, and released in Whip 0.3.42/code 48.

### DEC-20260903-008 — Make numeric quick adds an explicit Habit tracking-mode capability

- Context: Quick increment, preset amounts, and generated ranges only drive one-tap accumulation on the Count and Decimal Habit cards, but the editor exposed their configuration to all manual modes and the shared validator treated the increment as universally meaningful.
- Decision: `HabitTrackingMode.supportsQuickAddAmounts()` is the single semantic gate. Only unsynced Count and Decimal drafts render, parse, validate, and persist quick-add values. Every other mode persists the neutral non-null database representation (`1.0`, empty presets).
- Rationale: Visibility alone would leave hidden invalid state and alternate repository callers inconsistent. One small capability rule keeps UI, validation, and persistence aligned without introducing a generic behavior framework.
- Compatibility: No schema or historical-log change. Existing irrelevant presets on a nonnumeric Habit become invisible immediately and are removed the next time that Habit is saved; Count/Decimal behavior is unchanged.
- Related: `FB-20260903-008`, `FND-20260903-015`, `IMP-20260903-013`, `VER-20260903-013`.
- Status: Accepted, implemented, and pushed in `bc3de02`.

### DEC-20260903-009 — Conditional UI and durable semantics share one capability authority

- Context: Several editors correctly hid subordinate controls but continued validating or persisting their prior values. Gym also constructed option lists independently from the fields a selected tracking or machine type could produce.
- Decision: Model consequential capability and type rules in pure domain functions, normalize drafts at repository boundaries, and consume the same rules when presenting editor and analytics choices. Hidden fields retain no behavioral authority; completed-workout snapshots remain immutable.
- Rationale: Presentation-only conditionals cannot protect alternate callers, restored drafts, type changes, or legacy-shaped rows. A compact capability layer aligns UI, validation, and persistence without introducing a generic rules DSL.
- Compatibility: No schema or completed-history rewrite. Former At Most rows are interpreted correctly on read and canonicalized when edited; new writes remove irrelevant current-definition fields.
- Related: `FB-20260903-009`, `FND-20260903-016` through `FND-20260903-018`, `IMP-20260903-014`, `VER-20260903-014`.
- Status: Accepted, implemented, and focused-regression verified.
