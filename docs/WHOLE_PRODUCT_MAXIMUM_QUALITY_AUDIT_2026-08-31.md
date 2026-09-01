# Whip maximum-quality whole-product audit — 2026-08-31

Status: Iteration 1 discovery complete; foundational remediation in progress.

This document is the falsifiable evidence record for `FB-20260831-012`. It combines current-source inspection, independent specialist reviews, cross-agent challenge, a fresh disposable-emulator walkthrough, dynamic-type inspection, and the existing automated gate. It is not a release claim.

## Product direction

Whip should be a local-first personal execution and evidence system:

- Home, Areas, Search, Review, Settings, reminders, widgets, and backup form the common shell.
- Tasks, Habits, Goals, Tracks, and Gym remain domain-specific products rather than generic items with different labels.
- Daily action should be fast, history should remain truthful and user-owned, and advanced configuration should be progressively disclosed rather than removed.
- Gym's prescription snapshots and progression rules are useful precedent for future structured systems, but do not justify a universal programming DSL.

## Investigation method

- One Product Director coordinated independent productivity/ADHD/IA and accessibility/mobile/architecture/QA streams.
- The Director challenged severity, large-text navigation, Habit disclosure, legacy backup policy, and architectural scope before deciding.
- The root agent independently walked the current debug build on disposable emulator `emulator-5554` at 1080 × 2400, density 420, at 100% and 150% text.
- The physical Samsung phone remained connected but was not used for destructive testing.
- `scripts/check` completed successfully before remediation. Gradle reported JVM tests, Android-test compilation, lint, debug assembly, and coverage gates successful; several tasks were up-to-date, so this is a gate result rather than a claim of 884 freshly executed test methods.

Representative baseline evidence is under `artifacts/full-product-audit/2026-08-31/baseline/`, including:

- `home.png`, `tasks-stable.png`, `habits.png`, `goals.png`, `tracks.png`, and `gym-stable.png`
- `task-editor.png`, `habit-editor.png`, `goal-editor.png`, `track-editor.png`, and `goal-save-validation.png`
- every Settings category
- `large-text-home.png`, `large-text-gym.png`, and the large-text Exercise editor sequence

## Screen and workflow inventory

| Area | Current surfaces and workflows |
|---|---|
| Global | First-run setup; Home; six direct compact destinations; wide rail; Settings; global Add; Search; Review; Areas; keyboard shortcuts; fold/tabletop layouts |
| Home | Today; Tasks; Habits; Goals; Quick Log/Tracks; Gym; Review; section ordering/hiding/collapse; loading/error/retry; first-use and clear-day states |
| Tasks | Today, Inbox, Upcoming, History; List/Agenda/Calendar; quick capture; full editor; recurrence; reminders; subtasks; filters; grouping; reorder; bulk actions; Plan My Day; focus timer; inspector; occurrence history and recovery |
| Habits | Today, All, Archived, Insights; seven tracking modes; checklist/value/rating/timer logging; skips; pauses; history correction; Health sources; templates; editor; reminders; end conditions |
| Goals | Active, Completed, Archived, Insights; eight goal types; measurements; milestones; elapsed goals; templates; reminders; completion/abandon/reopen/archive/delete |
| Tracks | Tracks, Activity, Archived, Insights; Entries, Options, per-Track Insights; typed schemas; choice dependencies; entry editor; filters; CSV import/export; bulk/reorder |
| Gym | Active Workout, History, Progress, Library; Routines, Exercises, Machines, Categories, Tools; routine builder; 5/3/1 builder and cycle review; timers; set logging; records and analytics |
| Settings | Appearance & Home; Planning & Units; Organization; Reminders; Data & Privacy; About |
| Platform | Task/Habit widgets; Task/Habit/Goal reminders; focus/rest notifications; share-to-Task; exact-record deep links; Health Connect; portable backups |
| Persistence | Room v37; backup format 15; plain/encrypted export; preview/merge/replace; interrupted-restore recovery; CSV exports and Track CSV import |

## Persona workflow scorecard

These values are source- and emulator-informed heuristic estimates, not observed session timings. Discoverability and confidence use 5 as best; cognitive load and error risk use 5 as worst.

| Workflow | Discoverability | Cognitive load | Approximate cost | Confidence | Error risk |
|---|---:|---:|---:|---:|---:|
| Recommended first run | 5 | 1 | 1 tap | 4 | 1 |
| Today quick Task | 4 | 2 | 2–3 taps | 4 | 2 |
| Full recurring Task | 3 | 4 | 8–15 taps | 4 | 2 |
| Simple Habit | 4 | 4 | 3–6 taps plus long scroll | 3 | 3 |
| Complex Goal | 3 | 4 | 8–15 taps | 3 | 3 |
| Existing Track entry | 4 | 2 | 2–4 taps | 4 | 1 |
| Custom Track | 3 | 4 | 6+ taps | 4 | 3 |
| Exact-record Search | 5 | 2 | 2–3 taps | 5 | 1 |
| Returning through a clear Home | 2 | 3 | unclear | 2 | 3 |

## Findings

### P0 — Startup continues after unresolved restore recovery

- Observed: `WhipApplication.onCreate` wraps `RestoreRecoveryManager.recoverIfNeeded` in a discarded `runCatching`, then continues with default-Area creation and normal background initialization.
- Expected: unresolved recovery blocks normal UI, repositories, writes, scheduling, Health work, and widgets until Retry succeeds.
- Why it matters: user writes made after failed recovery can compound mixed state or later disappear when rollback succeeds.
- Affected users: anyone whose replace restore and rollback/rebuild are interrupted or corrupted.
- Reproduction: retain a recovery marker; corrupt its snapshot or inject rollback/rebuild failure; relaunch.
- Source: `WhipApplication.kt`, `RestoreRecoveryManager.kt`.
- Root cause: recovery was treated as best-effort startup housekeeping despite being the atomicity boundary for replace restore.
- Decision: fail closed with a blocking accessible recovery state, Retry, preserved marker, and non-mutating guidance.
- Regression gate: corrupt snapshot, rollback failure, rebuild failure, process restart, and proof that default initialization/background scheduling do not run.
- Remediation status: implemented and verified. A counted application-wide boundary blocks late operations, drains admitted work, serializes startup/live restore, rebuilds authoritative background state, and generation-scopes surviving actions, widgets, drafts, and imports. It preserves failed recovery markers and passed the complete 419-JVM/496-Android suite plus disposable-emulator corrupt-marker cold-launch testing.
- Durable record: `FND-20260831-007`, `DEC-20260831-008`.

### P1 — Workers can deliver obsolete Task, Habit, and Goal reminders

- Observed: the Task worker validates only existence/archive/task-level completion; the Habit worker can post when its domain projection is missing and omits dated pauses; the Goal worker checks only Active/deadline.
- Expected: immediately before posting, resolve the exact current logical occurrence, current reminder configuration, schedule version, pause/skip/completion state, and deadline. Missing or stale data fails closed.
- Why it matters: false reminders rapidly destroy trust, particularly for users relying on Whip as external executive-function support.
- Affected users: recurring Task, Habit, and Goal reminder users who edit, move, complete, skip, pause, disable, or delete near worker execution.
- Reproduction: allow queued work to race one of those mutations.
- Source: `ReminderWorker.kt`, `ReminderScheduler.kt`, `HabitReminderScheduler.kt`, `GoalReminderScheduler.kt`.
- Implemented solution: versioned exact delivery claims, domain-specific live target resolvers, definition-only fingerprints, exact idempotent actions, awaited WorkManager replacement, source/settings invalidation, a non-reentrant production mutation/resolve-post boundary, serialized full-snapshot Settings writes, legacy upgrade, and a durable deletion-cleanup journal spanning Room and visible platform notifications.
- Regression result: valid current delivery, stale/malformed/early work, scheduled/snoozed claims, complete/skip/move/pause/source-progress races, notification actions, mutation linearization, settings lost updates, rollback, process-interrupted deletion cleanup, and legacy upgrade are automated. All 451 JVM and 516 Android tests pass on the disposable API 34 emulator.
- Remediation status: resolved and verified in `IMP-20260831-005` / `VER-20260831-007`; no schema or backup-format change and no historical recomputation.
- Durable record: `FND-20260831-008`, `DEC-20260831-009`.

### P1 — Time and logical-date behavior is not uniformly tied to Whip's configured zone

- Observed:
  - follow-device zone changes do not trigger reminder/widget rescheduling;
  - Tracks snapshots `clock.today()` only when Track projections emit, so an open screen can retain yesterday across the logical cutoff;
  - Search completion dates and elapsed Goal detail use `ZoneId.systemDefault()` rather than the active Whip zone.
- Expected: live local-time behavior uses one explicit active-zone/current-date source; fixed-zone users stay fixed; saved historical local dates are not rewritten.
- Why it matters: reminders can fire at the wrong wall time, new Track entries can be dated incorrectly, and two Whip screens can disagree about the day.
- Affected users: travelers, shift workers, overnight users, pinned-zone users, and users with custom day cutoffs.
- Source: `SettingsViewModel.kt`, `AppSettings.kt`, `TrackViewModel.kt`, `UnifiedSearchDialog.kt`, `GoalScreens.kt`, `AndroidManifest.xml`.
- Recommended solution: current-date flow for Tracks, explicit active-zone inputs, and narrow time/timezone invalidation for follow-device schedules and widgets.
- Remediation status: partially implemented. `DATE_CHANGED`, `TIME_SET`, and `TIMEZONE_CHANGED` now trigger serialized reminder reconciliation with fixed-zone protection; the Track/Search/elapsed-Goal live-date consumers remain open.
- Regression gate: same-date zone changes, logical-date changes, custom cutoff, DST, fixed-zone no-op, and no repository emission.
- Durable record: `FND-20260831-009`, `DEC-20260831-010`.

### P1 — Failed productivity saves can change Area context before commit

- Observed: Task Save/Save & New, Habit Save, and Goal Save call `keepSavedItemVisible` before asynchronous persistence reports success.
- Expected: scope reconciliation and the “saved item” notice occur only after a confirmed successful commit; failures retain draft and current scope.
- Why it matters: a save failure can move the user's workspace and announce success for an item that does not exist.
- Affected users: everyone using Areas, especially interruption-prone and multi-Area workflows.
- Source: `WhipApp.kt`, `HabitScreens.kt`, `GoalScreens.kt`.
- Recommended solution: typed save results and success-only navigation/scope effects.
- Remediation status: resolved for Task/Habit/Goal authored definition editors with request-owned receipts, post-commit warning semantics, retained retry drafts, atomic admission, stale-terminal reclamation, input blocking, and authoritative Area fallback. Secondary mutation dialogs remain tracked separately as `FND-20260831-019`.
- Regression gate: injected failure for Task Save, Save & New, Habit, and Goal; editor/draft/scope/notice assertions.
- Durable record: `FND-20260831-010`, `DEC-20260831-011`.

### P1 — Enlarged text removes every visible primary navigation label

- Observed: compact navigation intentionally switches all six destinations to icon-only at font scale 1.5. The emulator evidence is `large-text-home.png` and `large-text-gym.png`.
- Expected: sighted low-vision and cognitive-accessibility users retain visible destination names through a measured one-row, stable two-row, or labeled-drawer layout.
- Why it matters: semantic descriptions do not help a sighted user interpret six unfamiliar glyphs; the Home mark is product-specific.
- Affected users: low-vision, ADHD/cognitive, first-time, narrow landscape, and 150–200% text users.
- Source: `WhipApp.kt` compact/rail navigation.
- Recommended solution: preserve direct named navigation when measured width/height permits; use a labeled drawer only at the final constrained fallback.
- Regression gate: 150%, 200%, 320dp, portrait, landscape, RTL, keyboard, deep link/back, and visible `Text` nodes.
- Durable record: `FND-20260831-011`, `DEC-20260831-012`.

### P1 — Basic Habit creation exposes advanced schedule machinery

- Observed: a simple Check Off Habit always traverses reminder overrides, end conditions, and first-day-of-week controls even when onboarding says advanced controls remain folded.
- Expected: name, tracking action, cadence, Area, and Save form the basic path; reminder state remains visible as a concise summary, while weekday overrides, ending, and week-boundary configuration use dependent disclosure.
- Why it matters: the form creates decision paralysis and context loss for novices and ADHD users without increasing capability.
- Affected users: first-time, casual, mobile, and executive-function users.
- Source: `HabitScreens.kt` editor schedule section.
- Recommended solution: hybrid disclosure that auto-expands for power mode, existing advanced data, or validation errors.
- Regression gate: simple/template/advanced existing Habit, validation auto-expansion, recreation, notifications, and 200% text.
- Durable record: `FND-20260831-012`, `DEC-20260831-013`.

### P1 — Settings can persist partial numeric and time edits

- Observed: parseable intermediate keystrokes are committed immediately. Replacing `300` with `600` can persist `6` or `60` if interrupted; clock parsing accepts extra segments such as `12:30:99`.
- Expected: fields own a local draft, validate the complete value strictly, and commit on IME Done or focus loss.
- Why it matters: interrupted editing silently changes reminders, cutoffs, rest times, backup retention, or Health lookback.
- Affected users: all Settings users, especially mobile and interruption-prone users.
- Source: `SettingsScreens.kt` numeric/time setting controls.
- Recommended solution: a shared transactional Settings field and typed validation outcome.
- Regression gate: replacement typing, invalid clocks, focus/IME commit, recreation, and rapid submission.
- Durable record: `FND-20260831-013`.

### P1 — Shared text is unbounded across Activity recreation

- Observed: `MainActivity` retains arbitrary external shared text in Activity state, and each extra line can become a subtask.
- Expected: title, notes, input bytes/characters, and subtask count have explicit budgets at the intent boundary, with visible truncation/refusal feedback.
- Why it matters: large shares can create memory pressure or `TransactionTooLargeException` during recreation.
- Affected users: share-to-Task users and apps sharing large documents/logs.
- Source: `MainActivity.kt`, `TaskEditorDialog.kt`.
- Regression gate: oversized share, thousands of lines, process/Activity recreation, Unicode boundaries, and normal shares.
- Durable record: `FND-20260831-014`.

### P1 — Public Goal completion history is omitted from backup

- Observed: `goal_completion_snapshots`, retained from public schema-27 builds as history, is omitted from backup; replace restore cascades those rows away.
- Expected: meaningful historical records survive backup/replace, either directly or through an explicit migration into the current historical representation.
- Why it matters: users can permanently lose evidence that exists in the installed database while being told backup is portable.
- Affected users: upgraded users with legacy completion snapshots.
- Source: `GoalEntities.kt`, `BackupRepository.kt`, backup tests.
- Recommended solution: preserve/migrate completion snapshots; keep redundant legacy tag-link internals omitted only after proving canonical equivalence.
- Regression gate: v27 → v37 → export → replace fidelity for plain and encrypted backups.
- Durable record: `FND-20260831-015`, `DEC-20260831-014`.

### P2 — Historical and returning-user information architecture has gaps

- Abandoned Goals appear in a destination labeled Completed. Rename the destination History/Closed and visibly distinguish Completed from Abandoned while preserving status and reopen behavior (`FND-20260831-016`).
- Existing users with only Inbox, upcoming, paused, recent, or unpinned content see a generic “Your Day Is Clear.” Add a small nonjudgmental “Pick up where you left off” path without turning Home into a guilt dashboard (`FND-20260831-017`).
- Track list query/no-match state is implemented but no list control can set it. Expose purposeful local search or remove the unreachable branch and intentionally route users to global Search (`FND-20260831-018`).

## Resolved dialectics

### Recovery severity

1. Position A: gate the ordinary Activity and cancel UI scopes; the durable marker and generation checks can protect later retries.
2. Position B: use one counted application-wide admission/drain barrier plus generation-scoped surviving state/actions.
3. Evidence and constraints: workers, receivers, schedulers, widgets, Health sync, configuration Activities, Activity-scoped ViewModels, SavedState drafts/imports, and cached entity references can all outlive visible composition. In-flight multi-step work should not be cancelled after partial progress.
4. Failure modes: Activity-only gating permits background writes and same-ID aliasing; broad cancellation loses drafts or strands partial work; one universal mutex adds latency/deadlock risk; a barrier without persistent generation tokens still admits stale aliases after reopening.
5. Synthesis/decision: P0; atomically deny late admission, drain admitted work, perform serialized recovery/restore and full rebuild, and invalidate generation-bound state before reopening access.
6. Why superior: Whip preserves both the last trustworthy database boundary and safely completed admitted operations without accepting mixed-generation mutations or a speculative whole-app rewrite.

### Large-text navigation

1. Position A: a labeled overflow avoids crowding.
2. Position B: six direct named destinations preserve muscle memory and one-tap access.
3. Evidence: direct labels fit at normal scale; at 150% current code removes all labels, though vertical space remains on tall phones.
4. Failure modes: unconditional overflow hides frequent domains; icon-only requires memorization; oversized one-row labels clip.
5. Decision: measured named row, then stable two-row named layout, then labeled drawer only when neither fits.
6. Why superior: it preserves stable access and makes enlarged text increase rather than remove comprehension.

### Habit progressive disclosure

1. Position A: hide reminders with all advanced fields.
2. Position B: keep reminders visible because they are a primary ADHD support.
3. Evidence: advanced weekday/end/week-boundary controls overload the basic path, but hidden reminder state is easy to forget.
4. Failure modes: hiding all reminder state reduces trust; exposing all machinery creates decision paralysis.
5. Decision: visible reminder summary plus disclosed configuration; auto-open for existing data, power mode, and errors.
6. Why superior: important state remains apparent while rare configuration no longer blocks simple creation.

### Legacy backup tables

1. Position A: export every retained table.
2. Position B: omit retired internals so obsolete semantics do not become a permanent API.
3. Evidence: completion snapshots are meaningful public history; tag links duplicate canonical tags.
4. Failure modes: blanket omission loses history; blanket inclusion fossilizes internals and can revive conflicting truth.
5. Decision: preserve or migrate completion snapshots; omit redundant tag links only after equivalence is proven.
6. Why superior: it protects user evidence without making every legacy table permanent product semantics.

### Architecture scope

1. Position A: consolidate routes and records into a generic item/program framework.
2. Position B: retain the current large domain screens.
3. Evidence: Task recurrence, Track schemas, Goal semantics, and Gym prescriptions have genuinely different invariants.
4. Failure modes: genericization erases domain meaning; leaving all state in giant roots perpetuates navigation and outcome bugs.
5. Decision: extract typed recovery, outcome, delivery-eligibility, time, and navigation seams only at demonstrated fault boundaries.
6. Why superior: it fixes systemic causes without a speculative universal DSL or whole-app rewrite.

## Positive behavior to preserve

- First-run recommended setup is clear, private-by-default, and one tap.
- Task occurrence history and “this and future” recurrence behavior are unusually strong.
- Draft preservation, explicit discard, Area identity, and exact-record Search routing are generally first-class.
- Track schema mutations acknowledge historical dependencies.
- Health-sourced measurements are not falsely presented as locally editable.
- Low-pressure Habit mode changes presentation without destroying history.
- Gym currently preserves arbitrary-lift 5/3/1, separate actual/e1RM/TM facts, additive Joker work, workout-only additions, exact routine/day return, timer boundaries, and immutable prescription snapshots.

## Implementation order

1. P0 recovery gate. Completed and verified in `VER-20260831-006`.
2. Reminder live-delivery integrity. Active.
3. Success-only productivity scope/navigation.
4. Unified time semantics.
5. Transactional Settings and bounded external share input.
6. Habit disclosure, named adaptive navigation, returning Home, Goal history, and Track search ownership.
7. Backup compatibility and scale.
8. Fresh focus-group retest, adversarial QA, full matrix, final review, and physical-device release.

Every completed behavior-level chunk must update durable memory, run targeted tests plus the proportional shared gate, be narrowly staged, committed, pushed normally, and verified reachable on `origin/main` before unrelated implementation begins.
